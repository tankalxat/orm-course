package com.ormcourse.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Getter
@Setter
@Entity
@Check(name = "ck_submissions_score", constraints = "score IS NULL OR score >= 0")
@Table(
        name = "submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_submissions_assignment_student",
                columnNames = {"assignment_id", "student_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_submissions_assignment"))
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_submissions_student"))
    private User student;

    @Column(nullable = false, length = 20000)
    private String content;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private boolean late;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    private Integer score;

    @Column(length = 4000)
    private String feedback;

    private Instant gradedAt;

    public Submission(Assignment assignment, User student, String content, Instant submittedAt) {
        this.assignment = assignment;
        this.student = student;
        this.content = content;
        this.submittedAt = submittedAt;
        this.late = assignment.isOverdueAt(submittedAt);
    }

    public void grade(int newScore, String newFeedback, Instant moment) {
        this.score = newScore;
        this.feedback = newFeedback;
        this.gradedAt = moment;
        this.status = SubmissionStatus.GRADED;
    }
}
