package com.ormcourse.platform.dto.assignment;

import com.ormcourse.platform.entity.Submission;
import com.ormcourse.platform.entity.SubmissionStatus;
import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        String content,
        Instant submittedAt,
        boolean late,
        SubmissionStatus status,
        Integer score,
        int maxScore,
        String feedback,
        Instant gradedAt
) {

    public static SubmissionResponse from(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getContent(),
                submission.getSubmittedAt(),
                submission.isLate(),
                submission.getStatus(),
                submission.getScore(),
                submission.getAssignment().getMaxScore(),
                submission.getFeedback(),
                submission.getGradedAt()
        );
    }
}
