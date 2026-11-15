package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.LessonCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    boolean existsByStudentIdAndLessonId(Long studentId, Long lessonId);
}
