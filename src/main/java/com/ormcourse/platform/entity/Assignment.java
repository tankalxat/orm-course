package com.ormcourse.platform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "assignments")
@Check(name = "ck_assignments_max_score", constraints = "max_score > 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assignments_lesson"))
    private Lesson lesson;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 10000)
    private String description;

    private Instant dueDate;

    @Column(nullable = false)
    private int maxScore;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "assignment", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Submission> submissions = new ArrayList<>();

    public Assignment(String title, String description, Instant dueDate, int maxScore) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxScore = maxScore;
    }

    public boolean isOverdueAt(Instant moment) {
        return dueDate != null && moment.isAfter(dueDate);
    }
}
