package com.ormcourse.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.dto.quiz.TakeQuizRequest;
import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.exception.ConflictException;
import com.ormcourse.platform.service.CourseService;
import com.ormcourse.platform.service.EnrollmentService;
import com.ormcourse.platform.service.LessonService;
import com.ormcourse.platform.service.ModuleService;
import com.ormcourse.platform.service.QuizAttemptService;
import com.ormcourse.platform.service.ReviewService;
import com.ormcourse.platform.service.TagService;
import com.ormcourse.platform.support.IntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourseCascadeIntegrationTest extends IntegrationTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private QuizAttemptService quizAttemptService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private TagService tagService;

    @Test
    void creatingCoursePersistsNestedModulesAndLessonsByCascade() {
        CourseDetailsResponse course = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 2, 3);

        assertThat(countRows("modules")).isEqualTo(2);
        assertThat(countRows("lessons")).isEqualTo(6);
        assertThat(course.modules()).extracting(m -> m.orderIndex()).containsExactly(1, 2);
        assertThat(course.modules().get(1).lessons()).hasSize(3);
    }

    @Test
    void deletingCourseWithoutStudentsRemovesWholeStructure() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        fixtures.assignment(course, null);
        fixtures.quiz(fixtures.firstModuleId(course), 3);

        courseService.delete(course.course().id());

        assertThat(countRows("courses")).isZero();
        assertThat(countRows("modules")).isZero();
        assertThat(countRows("lessons")).isZero();
        assertThat(countRows("assignments")).isZero();
        assertThat(countRows("quizzes")).isZero();
        assertThat(countRows("questions")).isZero();
        assertThat(countRows("answer_options")).isZero();
        assertThat(countRows("categories")).isEqualTo(1);
        assertThat(countRows("users")).isEqualTo(1);
    }

    @Test
    void deletingCourseWithEnrolledStudentsIsRejected() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        fixtures.enroll(course.course().id(), fixtures.student());

        assertThatThrownBy(() -> courseService.delete(course.course().id()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("archive");
        assertThat(countRows("courses")).isEqualTo(1);
    }

    @Test
    void deletingCourseAfterStudentsLeftRemovesTheirWorkResultsAndReviews() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long courseId = course.course().id();
        Long student = fixtures.student();
        fixtures.enroll(courseId, student);
        Long assignmentId = fixtures.assignment(course, Instant.now().plus(1, ChronoUnit.DAYS)).id();
        fixtures.submit(assignmentId, student);
        QuizResponse quiz = fixtures.quiz(fixtures.firstModuleId(course), 3);
        quizAttemptService.takeQuiz(quiz.id(), new TakeQuizRequest(student, fixtures.answers(quiz, 2)));
        lessonService.markCompleted(fixtures.firstLessonId(course), student);
        reviewService.add(courseId, new ReviewRequest(student, 5, "Great"));
        enrollmentService.unenroll(courseId, student);

        courseService.delete(courseId);

        assertThat(countRows("submissions")).isZero();
        assertThat(countRows("quiz_submissions")).isZero();
        assertThat(countRows("quiz_submission_answers")).isZero();
        assertThat(countRows("lesson_completions")).isZero();
        assertThat(countRows("course_reviews")).isZero();
        assertThat(countRows("users")).isEqualTo(2);
    }

    @Test
    void removingModuleDeletesItsLessonsAsOrphans() {
        CourseDetailsResponse course = fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 2, 2);

        moduleService.delete(fixtures.firstModuleId(course));

        assertThat(countRows("modules")).isEqualTo(1);
        assertThat(countRows("lessons")).isEqualTo(2);
        assertThat(courseService.getDetails(course.course().id()).modules()).hasSize(1);
    }

    @Test
    void tagsAreLinkedThroughJoinTableAndDetachedOnTagDeletion() {
        CourseDetailsResponse course = fixtures.publishedCourse();
        Long courseId = course.course().id();
        Long java = fixtures.tag("java");
        Long orm = fixtures.tag("orm");

        courseService.addTag(courseId, java);
        courseService.addTag(courseId, orm);

        assertThat(countRows("course_tag")).isEqualTo(2);
        assertThat(courseService.getDetails(courseId).course().tags()).containsExactly("java", "orm");

        courseService.removeTag(courseId, java);
        tagService.delete(orm);

        assertThat(countRows("course_tag")).isZero();
        assertThat(countRows("tags")).isEqualTo(1);
        assertThat(countRows("courses")).isEqualTo(1);
    }
}
