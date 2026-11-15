package com.ormcourse.platform.dto.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ModuleRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Positive Integer orderIndex,
        List<@Valid LessonRequest> lessons
) {

    public List<LessonRequest> lessonsOrEmpty() {
        return lessons == null ? List.of() : lessons;
    }
}
