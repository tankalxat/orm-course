package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.analytics.CoursePopularity;
import com.ormcourse.platform.dto.analytics.CourseProgressResponse;
import com.ormcourse.platform.dto.analytics.CourseStatisticsResponse;
import com.ormcourse.platform.dto.analytics.LeaderboardEntry;
import com.ormcourse.platform.dto.analytics.StudentProgressResponse;
import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.entity.EnrollmentStatus;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.repository.AnalyticsRepository;
import com.ormcourse.platform.repository.EnrollmentRepository;
import com.ormcourse.platform.repository.projection.CourseCount;
import com.ormcourse.platform.repository.projection.CourseScore;
import com.ormcourse.platform.repository.projection.QuizStats;
import com.ormcourse.platform.repository.projection.ReviewStats;
import com.ormcourse.platform.repository.projection.SubmissionStats;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int PERCENT = 100;

    private final AnalyticsRepository analyticsRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final UserLookup userLookup;

    public CourseStatisticsResponse courseStatistics(Long courseId) {
        Course course = courseService.find(courseId);
        ReviewStats reviews = analyticsRepository.reviewStats(courseId);
        SubmissionStats submissions = analyticsRepository.submissionStats(courseId);
        QuizStats quizzes = analyticsRepository.quizStats(courseId);
        return new CourseStatisticsResponse(
                courseId,
                course.getTitle(),
                enrollmentRepository.countByCourseId(courseId),
                enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE),
                enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.COMPLETED),
                reviews.count(),
                round(reviews.averageRating()),
                submissions.total(),
                submissions.graded(),
                round(submissions.averagePercent()),
                quizzes.attempts(),
                round(quizzes.passRatePercent())
        );
    }

    public StudentProgressResponse studentProgress(Long studentId) {
        User student = userLookup.getStudent(studentId);
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdOrderByEnrolledAtAsc(studentId);
        List<Long> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();

        if (courseIds.isEmpty()) {
            return new StudentProgressResponse(studentId, student.getName(), List.of());
        }

        ProgressData data = new ProgressData(
                countsByCourse(analyticsRepository.lessonsPerCourse(courseIds)),
                countsByCourse(analyticsRepository.completedLessonsPerCourse(studentId)),
                countsByCourse(analyticsRepository.assignmentsPerCourse(courseIds)),
                analyticsRepository.submissionsPerCourse(studentId).stream()
                        .collect(Collectors.toMap(CourseScore::courseId, Function.identity())),
                countsByCourse(analyticsRepository.quizzesPerCourse(courseIds)),
                countsByCourse(analyticsRepository.passedQuizzesPerCourse(studentId))
        );
        List<CourseProgressResponse> courses = enrollments.stream().map(e -> toProgress(e, data)).toList();
        return new StudentProgressResponse(studentId, student.getName(), courses);
    }

    public List<LeaderboardEntry> leaderboard(Long courseId, int limit) {
        courseService.find(courseId);
        return analyticsRepository.leaderboard(courseId, PageRequest.of(0, limit));
    }

    public List<CoursePopularity> popularCourses(int limit) {
        return analyticsRepository.mostPopularCourses(PageRequest.of(0, limit)).stream()
                .map(p -> new CoursePopularity(p.courseId(), p.courseTitle(), p.enrollments(), round(p.averageRating())))
                .toList();
    }

    private static CourseProgressResponse toProgress(Enrollment enrollment, ProgressData data) {
        Long courseId = enrollment.getCourse().getId();
        long lessonsTotal = data.lessons().getOrDefault(courseId, 0L);
        long lessonsCompleted = data.completedLessons().getOrDefault(courseId, 0L);
        CourseScore score = data.submissions().get(courseId);
        return new CourseProgressResponse(
                courseId,
                enrollment.getCourse().getTitle(),
                enrollment.getStatus(),
                lessonsCompleted,
                lessonsTotal,
                lessonsTotal == 0 ? 0 : (int) (lessonsCompleted * PERCENT / lessonsTotal),
                score == null ? 0 : score.count(),
                data.assignments().getOrDefault(courseId, 0L),
                score == null ? null : round(score.averagePercent()),
                data.passedQuizzes().getOrDefault(courseId, 0L),
                data.quizzes().getOrDefault(courseId, 0L)
        );
    }

    private static Map<Long, Long> countsByCourse(List<CourseCount> counts) {
        return counts.stream().collect(Collectors.toMap(CourseCount::courseId, CourseCount::count));
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * PERCENT) / (double) PERCENT;
    }

    private record ProgressData(
            Map<Long, Long> lessons,
            Map<Long, Long> completedLessons,
            Map<Long, Long> assignments,
            Map<Long, CourseScore> submissions,
            Map<Long, Long> quizzes,
            Map<Long, Long> passedQuizzes
    ) {
    }
}
