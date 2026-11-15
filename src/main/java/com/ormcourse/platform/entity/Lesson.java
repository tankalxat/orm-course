package com.ormcourse.platform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lessons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lessons_module"))
    private CourseModule module;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 20000)
    private String content;

    @Column(length = 500)
    private String videoUrl;

    @Column(nullable = false)
    private int orderIndex;

    private Integer durationMinutes;

    @Setter(AccessLevel.NONE)
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "lesson", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<LessonCompletion> completions = new ArrayList<>();

    public Lesson(String title, String content, int orderIndex) {
        this.title = title;
        this.content = content;
        this.orderIndex = orderIndex;
    }

    public void addAssignment(Assignment assignment) {
        assignments.add(assignment);
        assignment.setLesson(this);
    }
}
