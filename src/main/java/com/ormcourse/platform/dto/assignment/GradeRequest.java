package com.ormcourse.platform.dto.assignment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GradeRequest(@NotNull @PositiveOrZero Integer score, @Size(max = 4000) String feedback) {
}
