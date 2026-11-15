package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.enrollment.EnrollmentResponse;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.event.StudentEnrolledEvent;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.EnrollmentRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final UserLookup userLookup;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long studentId) {
        Course course = courseService.find(courseId);
        User student = userLookup.getStudent(studentId);

        if (!course.isOpenForEnrollment()) {
            throw new BusinessRuleException("Course " + courseId + " is " + course.getStatus() + " and not open for enrollment");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Student " + studentId + " is already enrolled in course " + courseId);
        }

        Enrollment enrollment = enrollmentRepository.save(new Enrollment(student, course, clock.instant()));
        eventPublisher.publishEvent(new StudentEnrolledEvent(studentId, course.getTitle()));
        log.info("Student id={} enrolled in course id={}", studentId, courseId);
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public void unenroll(Long courseId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new NotFoundException("Student " + studentId + " is not enrolled in course " + courseId));
        enrollmentRepository.delete(enrollment);
        log.info("Student id={} unenrolled from course id={}", studentId, courseId);
    }

    public List<EnrollmentResponse> listStudents(Long courseId) {
        courseService.find(courseId);
        return enrollmentRepository.findByCourseIdOrderByEnrolledAtAsc(courseId).stream().map(EnrollmentResponse::from).toList();
    }

    public List<EnrollmentResponse> listCourses(Long studentId) {
        userLookup.getStudent(studentId);
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtAsc(studentId).stream().map(EnrollmentResponse::from).toList();
    }

    @Transactional
    public EnrollmentResponse changeStatus(Long enrollmentId, EnrollmentStatus status) {
        Enrollment enrollment = enrollmentRepository.findDetailedById(enrollmentId)
                .orElseThrow(() -> NotFoundException.of("Enrollment", enrollmentId));
        enrollment.changeStatus(status, clock.instant());
        log.info("Enrollment id={} status changed to {}", enrollmentId, status);
        return EnrollmentResponse.from(enrollment);
    }
}
