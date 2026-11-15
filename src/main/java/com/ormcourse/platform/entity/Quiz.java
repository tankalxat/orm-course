package com.ormcourse.platform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Getter
@Setter
@Entity
@Table(name = "quizzes")
@Check(name = "ck_quizzes_passing_score", constraints = "passing_score BETWEEN 0 AND 100")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quiz extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_quizzes_module"))
    private CourseModule module;

    @Column(nullable = false, length = 200)
    private String title;

    private Integer timeLimitMinutes;

    @Column(nullable = false)
    private int passingScore;

    @Column(nullable = false)
    private int maxAttempts;

    @Setter(AccessLevel.NONE)
    @OrderBy("orderIndex ASC")
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<QuizSubmission> submissions = new ArrayList<>();

    public Quiz(String title, Integer timeLimitMinutes, int passingScore, int maxAttempts) {
        this.title = title;
        this.timeLimitMinutes = timeLimitMinutes;
        this.passingScore = passingScore;
        this.maxAttempts = maxAttempts;
    }

    public void addQuestion(Question question) {
        questions.add(question);
        question.setQuiz(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setQuiz(null);
    }
}
