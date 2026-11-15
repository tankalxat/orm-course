package com.ormcourse.platform.dto.quiz;

import com.ormcourse.platform.entity.AnswerOption;

public record AnswerOptionResponse(Long id, String text, Boolean correct) {

    public static AnswerOptionResponse from(AnswerOption option, boolean includeAnswers) {
        return new AnswerOptionResponse(option.getId(), option.getText(), includeAnswers ? option.isCorrect() : null);
    }
}
