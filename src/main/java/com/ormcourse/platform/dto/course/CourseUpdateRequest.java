package com.ormcourse.platform.dto.course;

import com.ormcourse.platform.entity.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CourseUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull Long categoryId,
        @NotNull Long teacherId,
        @Positive Integer durationHours,
        LocalDate startDate,
        @NotNull CourseStatus status
) {
}
