package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Quiz;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("""
            select q from Quiz q
            join fetch q.module m
            join fetch m.course
            where q.id = :id
            """)
    Optional<Quiz> findWithCourseById(@Param("id") Long id);

    @Query("""
            select q from Quiz q
            join fetch q.module m
            join fetch m.course
            left join fetch q.questions
            where q.id = :id
            """)
    Optional<Quiz> findWithQuestionsById(@Param("id") Long id);

    Optional<Quiz> findByModuleId(Long moduleId);

    boolean existsByModuleId(Long moduleId);

    long countByModuleCourseId(Long courseId);

    @Query("select q.module.course.teacher.id from Quiz q where q.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
