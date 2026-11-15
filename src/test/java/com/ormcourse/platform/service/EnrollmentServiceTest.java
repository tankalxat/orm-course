package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ormcourse.platform.dto.enrollment.EnrollmentResponse;
import com.ormcourse.platform.entity.Category;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.event.StudentEnrolledEvent;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.EnrollmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long STUDENT_ID = 20L;
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private UserLookup userLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private EnrollmentService service;
    private Course course;
    private User student;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(
                enrollmentRepository,
                courseService,
                userLookup,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        User teacher = new User("Teacher", "teacher@test.io", Role.TEACHER, "hash");
        course = new Course("Hibernate", null, new Category("ORM", null), teacher);
        ReflectionTestUtils.setField(course, "id", COURSE_ID);
        student = new User("Student", "student@test.io", Role.STUDENT, "hash");
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);
        lenient().when(courseService.find(COURSE_ID)).thenReturn(course);
    }

    @Test
    void enrollSavesEnrollmentWithClockTimeAndPublishesEvent() {
        course.setStatus(CourseStatus.PUBLISHED);
        when(userLookup.getStudent(STUDENT_ID)).thenReturn(student);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = service.enroll(COURSE_ID, STUDENT_ID);

        assertThat(response.enrolledAt()).isEqualTo(NOW);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        ArgumentCaptor<StudentEnrolledEvent> event = ArgumentCaptor.forClass(StudentEnrolledEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new StudentEnrolledEvent(STUDENT_ID, "Hibernate"));
    }

    @Test
    void draftCourseIsNotOpenForEnrollment() {
        course.setStatus(CourseStatus.DRAFT);
        when(userLookup.getStudent(STUDENT_ID)).thenReturn(student);

        assertThatThrownBy(() -> service.enroll(COURSE_ID, STUDENT_ID)).isInstanceOf(BusinessRuleException.class);
        verify(enrollmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void secondEnrollmentIsConflict() {
        course.setStatus(CourseStatus.PUBLISHED);
        when(userLookup.getStudent(STUDENT_ID)).thenReturn(student);
        when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.enroll(COURSE_ID, STUDENT_ID)).isInstanceOf(ConflictException.class);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void listStudentsFailsForUnknownCourse() {
        when(courseService.find(COURSE_ID)).thenThrow(NotFoundException.of("Course", COURSE_ID));

        assertThatThrownBy(() -> service.listStudents(COURSE_ID)).isInstanceOf(NotFoundException.class);
        verify(enrollmentRepository, never()).findByCourseIdOrderByEnrolledAtAsc(any());
    }

    @Test
    void unenrollWithoutEnrollmentIsNotFound() {
        when(enrollmentRepository.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unenroll(COURSE_ID, STUDENT_ID)).isInstanceOf(NotFoundException.class);
    }
}
