package com.ormcourse.platform.entity;

import java.util.Set;

public record QuizResult(int correctAnswers, int totalQuestions, int percentage, boolean passed, Set<AnswerOption> selectedOptions) {
}
