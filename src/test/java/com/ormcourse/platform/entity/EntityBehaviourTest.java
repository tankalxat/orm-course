package com.ormcourse.platform.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EntityBehaviourTest {

    private static final Instant DEADLINE = Instant.parse("2026-03-01T12:00:00Z");

    private final User teacher = new User("Teacher", "teacher@test.io", Role.TEACHER, "hash");
    private final User student = new User("Student", "student@test.io", Role.STUDENT, "hash");

    @Test
    void submissionAfterDeadlineIsLate() {
        Assignment assignment = new Assignment("Task", null, DEADLINE, 100);

        assertThat(new Submission(assignment, student, "x", DEADLINE).isLate()).isFalse();
        assertThat(new Submission(assignment, student, "x", DEADLINE.plus(1, ChronoUnit.MINUTES)).isLate()).isTrue();
        assertThat(new Submission(new Assignment("Open", null, null, 100), student, "x", DEADLINE).isLate()).isFalse();
    }

    @Test
    void gradingChangesSubmissionStatus() {
        Submission submission = new Submission(new Assignment("Task", null, null, 100), student, "x", DEADLINE);

        submission.grade(90, "Well done", DEADLINE);

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getScore()).isEqualTo(90);
        assertThat(submission.getGradedAt()).isEqualTo(DEADLINE);
    }

    @Test
    void completedAtIsSetOnlyForCompletedEnrollment() {
        Enrollment enrollment = new Enrollment(student, course(), DEADLINE);

        enrollment.changeStatus(EnrollmentStatus.COMPLETED, DEADLINE);
        assertThat(enrollment.getCompletedAt()).isEqualTo(DEADLINE);

        enrollment.changeStatus(EnrollmentStatus.ACTIVE, DEADLINE);
        assertThat(enrollment.getCompletedAt()).isNull();
    }

    @Test
    void courseHelpersKeepBothSidesOfAssociationsInSync() {
        Course course = course();
        Tag tag = new Tag("orm");
        CourseModule module = new CourseModule("Intro", null, 1);

        course.addTag(tag);
        course.addModule(module);

        assertThat(tag.getCourses()).containsExactly(course);
        assertThat(module.getCourse()).isSameAs(course);

        course.removeTag(tag);
        course.removeModule(module);

        assertThat(tag.getCourses()).isEmpty();
        assertThat(course.getModules()).isEmpty();
        assertThat(module.getCourse()).isNull();
    }

    @Test
    void onlyPublishedCourseIsOpenForEnrollment() {
        Course course = course();

        course.setStatus(CourseStatus.DRAFT);
        assertThat(course.isOpenForEnrollment()).isFalse();

        course.setStatus(CourseStatus.PUBLISHED);
        assertThat(course.isOpenForEnrollment()).isTrue();
    }

    @Test
    void quizAndModuleHelpersMaintainBackReferences() {
        CourseModule module = new CourseModule("Intro", null, 1);
        Quiz quiz = new Quiz("Check", null, 60, 1);
        Question question = new Question("Q", QuestionType.SINGLE_CHOICE, 1);

        module.attachQuiz(quiz);
        quiz.addQuestion(question);

        assertThat(quiz.getModule()).isSameAs(module);
        assertThat(question.getQuiz()).isSameAs(quiz);

        quiz.removeQuestion(question);
        module.detachQuiz();

        assertThat(quiz.getQuestions()).isEmpty();
        assertThat(module.getQuiz()).isNull();
    }

    @Test
    void correctOptionIdsContainOnlyCorrectOptions() {
        Question question = new Question("Q", QuestionType.MULTIPLE_CHOICE, 1);
        question.addOption(withId(new AnswerOption("a", true), 1L));
        question.addOption(withId(new AnswerOption("b", false), 2L));
        question.addOption(withId(new AnswerOption("c", true), 3L));

        assertThat(question.correctOptionIds()).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void entitiesAreEqualOnlyByAssignedIdentifierAndType() {
        Tag transientTag = new Tag("a");
        Tag first = withId(new Tag("a"), 1L);
        Tag sameId = withId(new Tag("b"), 1L);
        Category otherType = withId(new Category("a", null), 1L);

        assertThat(transientTag).isNotEqualTo(new Tag("a")).isEqualTo(transientTag);
        assertThat(first).isEqualTo(sameId).hasSameHashCodeAs(sameId);
        assertThat(first).isNotEqualTo(otherType).isNotEqualTo(null);
        assertThat(teacher.hasRole(Role.TEACHER)).isTrue();
        assertThat(teacher.hasRole(Role.STUDENT)).isFalse();
    }

    private Course course() {
        return new Course("Hibernate", null, new Category("ORM", null), teacher);
    }

    private static <T extends BaseEntity> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
