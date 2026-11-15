package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.Lesson;

public record LessonSummaryResponse(Long id, String title, int orderIndex, Integer durationMinutes, String videoUrl) {

    public static LessonSummaryResponse from(Lesson lesson) {
        return new LessonSummaryResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getOrderIndex(),
                lesson.getDurationMinutes(),
                lesson.getVideoUrl()
        );
    }
}
