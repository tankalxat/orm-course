package com.ormcourse.platform.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuizRequest(
        @NotBlank @Size(max = 200) String title,
        @Positive Integer timeLimitMinutes,
        @Min(0) @Max(100) Integer passingScore,
        @Min(1) @Max(10) Integer maxAttempts,
        List<@Valid QuestionRequest> questions
) {

    public static final int DEFAULT_PASSING_SCORE = 60;

    public static final int DEFAULT_MAX_ATTEMPTS = 3;

    public int passingScoreOrDefault() {
        return passingScore == null ? DEFAULT_PASSING_SCORE : passingScore;
    }

    public int maxAttemptsOrDefault() {
        return maxAttempts == null ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
    }

    public List<QuestionRequest> questionsOrEmpty() {
        return questions == null ? List.of() : questions;
    }
}
