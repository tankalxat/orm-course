package com.ormcourse.platform.event;

public record SubmissionGradedEvent(Long studentId, String assignmentTitle, int score, int maxScore) {
}
