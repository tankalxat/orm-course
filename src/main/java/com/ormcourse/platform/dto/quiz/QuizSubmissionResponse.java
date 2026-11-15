package com.ormcourse.platform.dto.quiz;

import com.ormcourse.platform.entity.QuizSubmission;
import java.time.Instant;

public record QuizSubmissionResponse(
        Long id,
        Long quizId,
        String quizTitle,
        Long studentId,
        String studentName,
        int score,
        int maxScore,
        int percentage,
        boolean passed,
        int attemptNumber,
        Instant takenAt
) {

    public static QuizSubmissionResponse from(QuizSubmission submission) {
        return new QuizSubmissionResponse(
                submission.getId(),
                submission.getQuiz().getId(),
                submission.getQuiz().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getScore(),
                submission.getMaxScore(),
                submission.getPercentage(),
                submission.isPassed(),
                submission.getAttemptNumber(),
                submission.getTakenAt()
        );
    }
}
