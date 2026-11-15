package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.assignment.AssignmentRequest;
import com.ormcourse.platform.dto.assignment.GradeRequest;
import com.ormcourse.platform.dto.assignment.StudentAssignmentResponse;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.entity.NotificationType;
import com.ormcourse.platform.entity.SubmissionStatus;
import com.ormcourse.platform.exception.BusinessRuleException;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.support.IntegrationTest;
import com.ormcourse.platform.support.TestFixtures;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubmissionServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private NotificationService notificationService;

    private CourseDetailsResponse course;
    private Long student;

    @BeforeEach
    void setUp() {
        course = fixtures.publishedCourse();
        student = fixtures.student();
        fixtures.enroll(course.course().id(), student);
    }

    @Test
    void newAssignmentNotifiesActiveStudents() {
        fixtures.assignment(course, null);

        assertThat(notificationService.listForUser(student, false))
                .extracting(n -> n.type())
                .contains(NotificationType.NEW_ASSIGNMENT);
    }

    @Test
    void submissionBeforeDeadlineIsOnTime() {
        Long assignmentId = fixtures.assignment(course, Instant.now().plus(1, ChronoUnit.DAYS)).id();

        SubmissionResponse submission = fixtures.submit(assignmentId, student);

        assertThat(submission.late()).isFalse();
        assertThat(submission.status()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    @Test
    void submissionAfterDeadlineIsMarkedLate() {
        Long assignmentId = fixtures.assignment(course, Instant.now().minus(1, ChronoUnit.HOURS)).id();

        assertThat(fixtures.submit(assignmentId, student).late()).isTrue();
    }

    @Test
    void studentCanSubmitAssignmentOnlyOnce() {
        Long assignmentId = fixtures.assignment(course, null).id();
        fixtures.submit(assignmentId, student);

        assertThatThrownBy(() -> fixtures.submit(assignmentId, student)).isInstanceOf(ConflictException.class);
    }

    @Test
    void studentWithoutEnrollmentCannotSubmit() {
        Long assignmentId = fixtures.assignment(course, null).id();

        assertThatThrownBy(() -> fixtures.submit(assignmentId, fixtures.student()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no active enrollment");
    }

    @Test
    void gradingStoresScoreFeedbackAndNotifiesStudent() {
        Long assignmentId = fixtures.assignment(course, null).id();
        Long submissionId = fixtures.submit(assignmentId, student).id();

        SubmissionResponse graded = submissionService.grade(submissionId, new GradeRequest(87, "Good"));

        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(graded.score()).isEqualTo(87);
        assertThat(graded.gradedAt()).isNotNull();
        assertThat(notificationService.listForUser(student, false))
                .anySatisfy(n -> assertThat(n.message()).contains("87/" + TestFixtures.MAX_SCORE));
    }

    @Test
    void scoreAboveMaximumIsRejected() {
        Long assignmentId = fixtures.assignment(course, null).id();
        Long submissionId = fixtures.submit(assignmentId, student).id();

        assertThatThrownBy(() -> submissionService.grade(submissionId, new GradeRequest(TestFixtures.MAX_SCORE + 1, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void ungradedSubmissionCanBeWithdrawnButGradedCannot() {
        Long first = fixtures.assignment(course, null).id();
        Long second = fixtures.assignment(course, null).id();
        Long ungraded = fixtures.submit(first, student).id();
        Long graded = fixtures.submit(second, student).id();
        submissionService.grade(graded, new GradeRequest(50, null));

        submissionService.withdraw(ungraded);

        assertThatThrownBy(() -> submissionService.withdraw(graded)).isInstanceOf(ConflictException.class);
        assertThat(submissionService.listByStudent(student)).extracting(SubmissionResponse::id).containsExactly(graded);
    }

    @Test
    void studentSeesAssignmentsOfHisCoursesWithOwnSubmissions() {
        Long submitted = fixtures.assignment(course, null).id();
        Long pending = fixtures.assignment(course, null).id();
        fixtures.submit(submitted, student);

        List<StudentAssignmentResponse> assignments = assignmentService.listForStudent(student);

        assertThat(assignments).hasSize(2);
        assertThat(assignments).filteredOn(a -> a.assignment().id().equals(submitted)).singleElement()
                .satisfies(a -> assertThat(a.submission()).isNotNull());
        assertThat(assignments).filteredOn(a -> a.assignment().id().equals(pending)).singleElement()
                .satisfies(a -> assertThat(a.submission()).isNull());
    }

    @Test
    void deletingAssignmentRemovesItsSubmissions() {
        Long assignmentId = fixtures.assignment(course, null).id();
        fixtures.submit(assignmentId, student);
        AssignmentRequest update = new AssignmentRequest("Renamed", "New", null, 10);

        assertThat(assignmentService.update(assignmentId, update).title()).isEqualTo("Renamed");

        assignmentService.delete(assignmentId);

        assertThat(countRows("assignments")).isZero();
        assertThat(countRows("submissions")).isZero();
    }
}
