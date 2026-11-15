package com.ormcourse.platform.repository.projection;

public record CourseScore(Long courseId, Long count, Double averagePercent) {
}
