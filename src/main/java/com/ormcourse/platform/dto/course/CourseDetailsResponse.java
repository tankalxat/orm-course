package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseModule;
import java.time.Instant;
import java.util.List;

public record CourseDetailsResponse(
        CourseSummaryResponse course,
        String description,
        Instant createdAt,
        Instant updatedAt,
        List<ModuleResponse> modules
) {

    public static CourseDetailsResponse from(Course course, List<CourseModule> modules) {
        return new CourseDetailsResponse(
                CourseSummaryResponse.from(course),
                course.getDescription(),
                course.getCreatedAt(),
                course.getUpdatedAt(),
                modules.stream().map(ModuleResponse::from).toList()
        );
    }
}
