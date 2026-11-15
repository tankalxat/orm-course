package com.ormcourse.platform.event;

public record AssignmentCreatedEvent(Long courseId, String courseTitle, String assignmentTitle) {
}
