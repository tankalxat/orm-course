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

@Getter
@Entity
@Table(
        name = "lesson_completions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_completions_student_lesson",
                columnNames = {"student_id", "lesson_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LessonCompletion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lesson_completions_student"))
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lesson_completions_lesson"))
    private Lesson lesson;

    @Column(nullable = false)
    private Instant completedAt;

    public LessonCompletion(User student, Lesson lesson, Instant completedAt) {
        this.student = student;
        this.lesson = lesson;
        this.completedAt = completedAt;
    }
}
