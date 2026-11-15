package com.ormcourse.platform.dto.assignment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AssignmentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        Instant dueDate,
        @NotNull @Positive @Max(1000) Integer maxScore
) {
}
