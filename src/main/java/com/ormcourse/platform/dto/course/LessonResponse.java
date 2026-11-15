package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.dto.assignment.AssignmentResponse;
import com.ormcourse.platform.entity.Lesson;
import java.util.List;

public record LessonResponse(
        Long id,
        Long moduleId,
        Long courseId,
        String title,
        String content,
        String videoUrl,
        int orderIndex,
        Integer durationMinutes,
        List<AssignmentResponse> assignments
) {

    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getModule().getId(),
                lesson.getModule().getCourse().getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getVideoUrl(),
                lesson.getOrderIndex(),
                lesson.getDurationMinutes(),
                lesson.getAssignments().stream().map(AssignmentResponse::from).toList()
        );
    }
}
