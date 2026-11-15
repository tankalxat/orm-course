package com.ormcourse.platform.dto.analytics;

import java.util.List;

public record StudentProgressResponse(Long studentId, String studentName, List<CourseProgressResponse> courses) {
}
