package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.QuizSubmission;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    int countByQuizIdAndStudentId(Long quizId, Long studentId);

    boolean existsByQuizId(Long quizId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByQuizIdOrderByTakenAtAsc(Long quizId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByStudentIdOrderByTakenAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    @Query("""
            select qs from QuizSubmission qs
            where qs.quiz.module.course.id = :courseId
            order by qs.takenAt asc
            """)
    List<QuizSubmission> findByCourseId(@Param("courseId") Long courseId);
}
