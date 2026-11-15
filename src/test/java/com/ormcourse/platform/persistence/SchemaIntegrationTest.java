package com.ormcourse.platform.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseReview;
import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.entity.Quiz;
import com.ormcourse.platform.entity.QuizSubmission;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.repository.CourseRepository;
import com.ormcourse.platform.repository.CourseReviewRepository;
import com.ormcourse.platform.repository.EnrollmentRepository;
import com.ormcourse.platform.repository.QuizRepository;
import com.ormcourse.platform.repository.QuizSubmissionRepository;
import com.ormcourse.platform.repository.UserRepository;
import com.ormcourse.platform.support.IntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class SchemaIntegrationTest extends IntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseReviewRepository reviewRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizSubmissionRepository quizSubmissionRepository;

    @Test
    void domainModelContainsBetweenFifteenAndTwentyEntities() {
        int entities = entityManagerFactory.getMetamodel().getEntities().size();

        assertThat(entities).isBetween(15, 20);
    }

    @Test
    void schemaContainsEntityAndJoinTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = 'public'",
                String.class
        );

        assertThat(tables).contains(
                "users", "profiles", "categories", "tags", "courses", "course_tag", "modules", "lessons",
                "assignments", "submissions", "enrollments", "quizzes", "questions", "answer_options",
                "quiz_submissions", "quiz_submission_answers", "course_reviews", "lesson_completions", "notifications"
        );
    }

    @Test
    void schemaDeclaresNamedForeignKeysAndUniqueConstraints() {
        List<String> constraints = jdbcTemplate.queryForList(
                "select constraint_name from information_schema.table_constraints "
                        + "where table_schema = 'public' and constraint_type in ('FOREIGN KEY', 'UNIQUE')",
                String.class
        );

        assertThat(constraints).contains(
                "fk_courses_category",
                "fk_courses_teacher",
                "fk_course_tag_course",
                "fk_course_tag_tag",
                "uk_users_email",
                "uk_enrollments_student_course",
                "uk_submissions_assignment_student",
                "uk_course_reviews_course_student",
                "uk_quiz_submissions_quiz_student_attempt"
        );
    }

    @Test
    void databaseRejectsDuplicateEnrollmentEvenWithoutServiceCheck() {
        CourseDetailsResponse details = fixtures.publishedCourse();
        User student = userRepository.findById(fixtures.student()).orElseThrow();
        Course course = courseRepository.findById(details.course().id()).orElseThrow();
        enrollmentRepository.saveAndFlush(new Enrollment(student, course, Instant.now()));

        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(new Enrollment(student, course, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseCheckConstraintRejectsRatingOutOfRange() {
        CourseDetailsResponse details = fixtures.publishedCourse();
        User student = userRepository.findById(fixtures.student()).orElseThrow();
        Course course = courseRepository.findById(details.course().id()).orElseThrow();

        assertThatThrownBy(() -> reviewRepository.saveAndFlush(new CourseReview(course, student, 6, "too good")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateQuizAttemptNumber() {
        CourseDetailsResponse details = fixtures.publishedCourse();
        User student = userRepository.findById(fixtures.student()).orElseThrow();
        Quiz quiz = quizRepository.findById(fixtures.quiz(fixtures.firstModuleId(details), 3).id()).orElseThrow();
        quizSubmissionRepository.saveAndFlush(new QuizSubmission(quiz, student, 1, Instant.now()));

        assertThatThrownBy(() -> quizSubmissionRepository.saveAndFlush(new QuizSubmission(quiz, student, 1, Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
