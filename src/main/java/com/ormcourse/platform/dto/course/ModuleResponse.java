package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.CourseModule;
import java.util.List;

public record ModuleResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        int orderIndex,
        Long quizId,
        List<LessonSummaryResponse> lessons
) {

    public static ModuleResponse from(CourseModule module) {
        return new ModuleResponse(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getDescription(),
                module.getOrderIndex(),
                module.getQuiz() == null ? null : module.getQuiz().getId(),
                module.getLessons().stream().map(LessonSummaryResponse::from).toList()
        );
    }
}
