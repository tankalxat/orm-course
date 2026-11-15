package com.ormcourse.platform.event;

public record QuizCompletedEvent(Long studentId, String quizTitle, int percentage, boolean passed) {
}
