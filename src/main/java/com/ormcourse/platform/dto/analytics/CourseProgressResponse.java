package com.ormcourse.platform.dto.analytics;

import com.ormcourse.platform.entity.EnrollmentStatus;

public record CourseProgressResponse(
        Long courseId,
        String courseTitle,
        EnrollmentStatus enrollmentStatus,
        long lessonsCompleted,
        long lessonsTotal,
        int progressPercent,
        long assignmentsSubmitted,
        long assignmentsTotal,
        Double averageScorePercent,
        long quizzesPassed,
        long quizzesTotal
) {
}
