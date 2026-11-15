package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.enrollment.EnrollmentResponse;
import com.ormcourse.platform.dto.notification.NotificationResponse;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.entity.NotificationType;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private NotificationService notificationService;

    @Test
    void enrollCreatesActiveEnrollmentAndNotifiesStudent() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long student = fixtures.student();

        EnrollmentResponse enrollment = enrollmentService.enroll(course.course().id(), student);

        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.courseTitle()).isEqualTo(course.course().title());
        List<NotificationResponse> notifications = notificationService.listForUser(student, true);
        assertThat(notifications).singleElement()
                .satisfies(n -> assertThat(n.type()).isEqualTo(NotificationType.ENROLLED))
                .satisfies(n -> assertThat(n.message()).contains(course.course().title()));
    }

    @Test
    void secondEnrollmentToSameCourseIsRejected() {
        Long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();
        enrollmentService.enroll(courseId, student);

        assertThatThrownBy(() -> enrollmentService.enroll(courseId, student)).isInstanceOf(ConflictException.class);
    }

    @Test
    void draftCourseIsClosedForEnrollment() {
        Long courseId = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 1, 1).course().id();

        assertThatThrownBy(() -> enrollmentService.enroll(courseId, fixtures.student()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void onlyStudentsCanEnroll() {
        Long courseId = fixtures.publishedCourse().course().id();

        assertThatThrownBy(() -> enrollmentService.enroll(courseId, fixtures.teacher()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("STUDENT");
    }

    @Test
    void unenrollRemovesEnrollmentAndAllowsToEnrollAgain() {
        Long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();
        enrollmentService.enroll(courseId, student);

        enrollmentService.unenroll(courseId, student);

        assertThat(enrollmentService.listStudents(courseId)).isEmpty();
        assertThat(enrollmentService.enroll(courseId, student).status()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    void unenrollWithoutEnrollmentThrowsNotFound() {
        Long courseId = fixtures.publishedCourse().course().id();

        assertThatThrownBy(() -> enrollmentService.unenroll(courseId, fixtures.student()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void completingEnrollmentSetsCompletionDate() {
        Long courseId = fixtures.publishedCourse().course().id();
        Long student = fixtures.student();
        Long enrollmentId = enrollmentService.enroll(courseId, student).id();

        EnrollmentResponse completed = enrollmentService.changeStatus(enrollmentId, EnrollmentStatus.COMPLETED);

        assertThat(completed.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
        assertThat(enrollmentService.listCourses(student)).extracting(EnrollmentResponse::courseId).containsExactly(courseId);
    }
}
