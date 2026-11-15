package com.ormcourse.platform.dto.assignment;

public record StudentAssignmentResponse(
        Long courseId,
        String courseTitle,
        AssignmentResponse assignment,
        SubmissionResponse submission
) {
}
