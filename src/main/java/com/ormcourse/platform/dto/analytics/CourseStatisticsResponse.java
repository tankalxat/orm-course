package com.ormcourse.platform.dto.analytics;

public record CourseStatisticsResponse(
        Long courseId,
        String courseTitle,
        long totalEnrollments,
        long activeEnrollments,
        long completedEnrollments,
        long reviewsCount,
        Double averageRating,
        long submissionsCount,
        long gradedSubmissionsCount,
        Double averageScorePercent,
        long quizAttempts,
        Double quizPassRatePercent
) {
}
