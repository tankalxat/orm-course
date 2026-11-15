package com.ormcourse.platform.dto.quiz;

import com.ormcourse.platform.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionRequest(
        @NotBlank @Size(max = 1000) String text,
        @NotNull QuestionType type,
        @NotNull @Size(min = 2, max = 10) List<@Valid @NotNull AnswerOptionRequest> options
) {

    @AssertTrue(message = "single choice question must have exactly one correct option, multiple choice at least one")
    public boolean isCorrectOptionsConsistent() {
        if (options == null || type == null) {
            return true;
        }

        long correct = options.stream().filter(o -> o != null && o.correct()).count();
        return type == QuestionType.SINGLE_CHOICE ? correct == 1 : correct >= 1;
    }
}
