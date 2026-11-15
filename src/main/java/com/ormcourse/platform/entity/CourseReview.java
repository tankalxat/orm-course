package com.ormcourse.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Check(name = "ck_course_reviews_rating", constraints = "rating BETWEEN 1 AND 5")
@Table(
        name = "course_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_reviews_course_student",
                columnNames = {"course_id", "student_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_reviews_course"))
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_course_reviews_student"))
    private User student;

    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String comment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public CourseReview(Course course, User student, int rating, String comment) {
        this.course = course;
        this.student = student;
        this.rating = rating;
        this.comment = comment;
    }
}
