package com.ormcourse.platform.dto.quiz;

import com.ormcourse.platform.entity.Question;
import com.ormcourse.platform.entity.Quiz;
import java.util.List;

public record QuizResponse(
        Long id,
        Long moduleId,
        Long courseId,
        String title,
        Integer timeLimitMinutes,
        int passingScore,
        int maxAttempts,
        List<QuestionResponse> questions
) {

    public static QuizResponse from(Quiz quiz, List<Question> questions, boolean includeAnswers) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getModule().getId(),
                quiz.getModule().getCourse().getId(),
                quiz.getTitle(),
                quiz.getTimeLimitMinutes(),
                quiz.getPassingScore(),
                quiz.getMaxAttempts(),
                questions.stream().map(q -> QuestionResponse.from(q, includeAnswers)).toList()
        );
    }
}
