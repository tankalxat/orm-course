package com.ormcourse.platform.dto.enrollment;

import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.entity.EnrollmentStatus;
import java.time.Instant;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentName,
        Long courseId,
        String courseTitle,
        Instant enrolledAt,
        EnrollmentStatus status,
        Instant completedAt
) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getEnrolledAt(),
                enrollment.getStatus(),
                enrollment.getCompletedAt()
        );
    }
}
