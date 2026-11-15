package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.CourseStatus;

public record CourseSearchFilter(Long categoryId, Long teacherId, CourseStatus status, String tag, String query) {
}
