package com.ormcourse.platform.dto.quiz;

import com.ormcourse.platform.entity.Question;
import com.ormcourse.platform.entity.QuestionType;
import java.util.List;

public record QuestionResponse(Long id, String text, QuestionType type, int orderIndex, List<AnswerOptionResponse> options) {

    public static QuestionResponse from(Question question, boolean includeAnswers) {
        return new QuestionResponse(
                question.getId(),
                question.getText(),
                question.getType(),
                question.getOrderIndex(),
                question.getOptions().stream().map(o -> AnswerOptionResponse.from(o, includeAnswers)).toList()
        );
    }
}
