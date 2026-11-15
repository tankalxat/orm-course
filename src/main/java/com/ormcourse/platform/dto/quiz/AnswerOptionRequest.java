package com.ormcourse.platform.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerOptionRequest(@NotBlank @Size(max = 500) String text, boolean correct) {
}
