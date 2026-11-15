package com.ormcourse.platform.dto.quiz;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

public record TakeQuizRequest(@NotNull Long studentId, @NotNull Map<@NotNull Long, @NotEmpty Set<@NotNull Long>> answers) {
}
