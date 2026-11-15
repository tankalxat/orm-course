package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.Course;
import com.ormcourse.platform.entity.CourseStatus;
import com.ormcourse.platform.entity.Tag;
import java.time.LocalDate;
import java.util.List;

public record CourseSummaryResponse(
        Long id,
        String title,
        Long categoryId,
        String categoryName,
        Long teacherId,
        String teacherName,
        CourseStatus status,
        Integer durationHours,
        LocalDate startDate,
        List<String> tags
) {

    public static CourseSummaryResponse from(Course course) {
        return new CourseSummaryResponse(
                course.getId(),
                course.getTitle(),
                course.getCategory().getId(),
                course.getCategory().getName(),
                course.getTeacher().getId(),
                course.getTeacher().getName(),
                course.getStatus(),
                course.getDurationHours(),
                course.getStartDate(),
                course.getTags().stream().map(Tag::getName).sorted().toList()
        );
    }
}
