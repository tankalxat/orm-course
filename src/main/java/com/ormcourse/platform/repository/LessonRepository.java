package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Lesson;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("""
            select l from Lesson l
            join fetch l.module m
            join fetch m.course
            left join fetch l.assignments
            where l.id = :id
            """)
    Optional<Lesson> findWithAssignmentsById(@Param("id") Long id);

    @Query("select l from Lesson l join fetch l.module m join fetch m.course where l.id = :id")
    Optional<Lesson> findWithCourseById(@Param("id") Long id);

    @Query("select coalesce(max(l.orderIndex), 0) from Lesson l where l.module.id = :moduleId")
    int findMaxOrderIndex(@Param("moduleId") Long moduleId);

    long countByModuleCourseId(Long courseId);

    @Query("select l.module.course.teacher.id from Lesson l where l.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
