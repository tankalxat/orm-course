package com.ormcourse.platform.dto.assignment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRequest(@NotNull Long studentId, @NotBlank @Size(max = 20000) String content) {
}
