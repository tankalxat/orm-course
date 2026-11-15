package com.ormcourse.platform.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record LessonRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 20000) String content,
        @URL @Size(max = 500) String videoUrl,
        @Positive Integer orderIndex,
        @Positive Integer durationMinutes
) {
}
