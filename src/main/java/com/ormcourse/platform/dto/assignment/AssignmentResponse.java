package com.ormcourse.platform.dto.assignment;

import com.ormcourse.platform.entity.Assignment;
import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long lessonId,
        String title,
        String description,
        Instant dueDate,
        int maxScore,
        Instant createdAt
) {

    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getLesson().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxScore(),
                assignment.getCreatedAt()
        );
    }
}
