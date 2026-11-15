package com.ormcourse.platform.dto.review;

import com.ormcourse.platform.entity.CourseReview;
import java.time.Instant;

public record ReviewResponse(Long id, Long courseId, Long studentId, String studentName, int rating, String comment, Instant createdAt) {

    public static ReviewResponse from(CourseReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getCourse().getId(),
                review.getStudent().getId(),
                review.getStudent().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
