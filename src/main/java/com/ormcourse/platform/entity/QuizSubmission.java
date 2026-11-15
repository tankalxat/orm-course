package com.ormcourse.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "quiz_submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_submissions_quiz_student_attempt",
                columnNames = {"quiz_id", "student_id", "attempt_number"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quiz_submissions_quiz"))
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_quiz_submissions_student"))
    private User student;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int maxScore;

    @Column(nullable = false)
    private int percentage;

    @Column(nullable = false)
    private boolean passed;

    @Column(nullable = false)
    private int attemptNumber;

    @Column(nullable = false)
    private Instant takenAt;

    @Setter(AccessLevel.NONE)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quiz_submission_answers",
            joinColumns = @JoinColumn(
                    name = "quiz_submission_id",
                    foreignKey = @ForeignKey(name = "fk_quiz_submission_answers_submission")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "answer_option_id",
                    foreignKey = @ForeignKey(name = "fk_quiz_submission_answers_option")
            )
    )
    private Set<AnswerOption> selectedOptions = new HashSet<>();

    public QuizSubmission(Quiz quiz, User student, int attemptNumber, Instant takenAt) {
        this.quiz = quiz;
        this.student = student;
        this.attemptNumber = attemptNumber;
        this.takenAt = takenAt;
    }

    public void applyResult(QuizResult result) {
        this.score = result.correctAnswers();
        this.maxScore = result.totalQuestions();
        this.percentage = result.percentage();
        this.passed = result.passed();
        this.selectedOptions.addAll(result.selectedOptions());
    }
}
