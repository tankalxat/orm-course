package com.ormcourse.platform.dto.analytics;

public record CoursePopularity(Long courseId, String courseTitle, Long enrollments, Double averageRating) {
}
