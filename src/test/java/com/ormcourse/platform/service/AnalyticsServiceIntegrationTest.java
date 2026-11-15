package com.ormcourse.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ormcourse.platform.dto.analytics.CoursePopularity;
import com.ormcourse.platform.dto.analytics.CourseProgressResponse;
import com.ormcourse.platform.dto.analytics.CourseStatisticsResponse;
import com.ormcourse.platform.dto.analytics.LeaderboardEntry;
import com.ormcourse.platform.dto.analytics.StudentProgressResponse;
import com.ormcourse.platform.dto.assignment.GradeRequest;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.dto.quiz.TakeQuizRequest;
import com.ormcourse.platform.dto.review.ReviewRequest;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnalyticsServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private QuizAttemptService quizAttemptService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private LessonService lessonService;

    @Autowired
    private EnrollmentService enrollmentService;

    private CourseDetailsResponse course;
    private Long courseId;
    private Long best;
    private Long average;

    @BeforeEach
    void setUp() {
        course = fixtures.publishedCourse();
        courseId = course.course().id();
        best = fixtures.student();
        average = fixtures.student();
        fixtures.enroll(courseId, best);
        fixtures.enroll(courseId, average);
        Long assignmentId = fixtures.assignment(course, null).id();
        submissionService.grade(fixtures.submit(assignmentId, best).id(), new GradeRequest(90, null));
        submissionService.grade(fixtures.submit(assignmentId, average).id(), new GradeRequest(60, null));
        QuizResponse quiz = fixtures.quiz(fixtures.firstModuleId(course), 3);
        quizAttemptService.takeQuiz(quiz.id(), new TakeQuizRequest(best, fixtures.answers(quiz, 2)));
        quizAttemptService.takeQuiz(quiz.id(), new TakeQuizRequest(average, fixtures.answers(quiz, 0)));
        reviewService.add(courseId, new ReviewRequest(best, 5, null));
        reviewService.add(courseId, new ReviewRequest(average, 4, null));
        lessonService.markCompleted(fixtures.firstLessonId(course), best);
    }

    @Test
    void courseStatisticsAggregateEnrollmentsReviewsScoresAndQuizzes() {
        enrollmentService.changeStatus(enrollmentService.listStudents(courseId).get(0).id(), EnrollmentStatus.COMPLETED);

        CourseStatisticsResponse statistics = analyticsService.courseStatistics(courseId);

        assertThat(statistics.totalEnrollments()).isEqualTo(2);
        assertThat(statistics.activeEnrollments()).isEqualTo(1);
        assertThat(statistics.completedEnrollments()).isEqualTo(1);
        assertThat(statistics.reviewsCount()).isEqualTo(2);
        assertThat(statistics.averageRating()).isEqualTo(4.5);
        assertThat(statistics.submissionsCount()).isEqualTo(2);
        assertThat(statistics.gradedSubmissionsCount()).isEqualTo(2);
        assertThat(statistics.averageScorePercent()).isEqualTo(75.0);
        assertThat(statistics.quizAttempts()).isEqualTo(2);
        assertThat(statistics.quizPassRatePercent()).isEqualTo(50.0);
    }

    @Test
    void leaderboardIsOrderedByTotalScore() {
        List<LeaderboardEntry> leaderboard = analyticsService.leaderboard(courseId, 10);

        assertThat(leaderboard).extracting(LeaderboardEntry::studentId).containsExactly(best, average);
        assertThat(leaderboard.get(0).totalScore()).isEqualTo(90L);
        assertThat(analyticsService.leaderboard(courseId, 1)).hasSize(1);
    }

    @Test
    void studentProgressShowsLessonsAssignmentsAndQuizzesPerCourse() {
        StudentProgressResponse progress = analyticsService.studentProgress(best);

        CourseProgressResponse courseProgress = progress.courses().get(0);
        assertThat(courseProgress.lessonsCompleted()).isEqualTo(1);
        assertThat(courseProgress.lessonsTotal()).isEqualTo(2);
        assertThat(courseProgress.progressPercent()).isEqualTo(50);
        assertThat(courseProgress.assignmentsSubmitted()).isEqualTo(1);
        assertThat(courseProgress.assignmentsTotal()).isEqualTo(1);
        assertThat(courseProgress.averageScorePercent()).isEqualTo(90.0);
        assertThat(courseProgress.quizzesPassed()).isEqualTo(1);
        assertThat(courseProgress.quizzesTotal()).isEqualTo(1);
    }

    @Test
    void studentWithoutCoursesHasEmptyProgress() {
        assertThat(analyticsService.studentProgress(fixtures.student()).courses()).isEmpty();
    }

    @Test
    void popularCoursesAreOrderedByEnrollments() {
        Long emptyCourse = fixtures.publishedCourse().course().id();
        fixtures.course(fixtures.teacher(), CourseStatus.DRAFT, 1, 1);

        List<CoursePopularity> popular = analyticsService.popularCourses(5);

        assertThat(popular).extracting(CoursePopularity::courseId).containsExactly(courseId, emptyCourse);
        assertThat(popular.get(0).enrollments()).isEqualTo(2L);
        assertThat(popular.get(0).averageRating()).isEqualTo(4.5);
        assertThat(popular.get(1).averageRating()).isNull();
    }
}
