package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("""
            select a from Assignment a
            join fetch a.lesson l
            join fetch l.module m
            join fetch m.course
            where a.id = :id
            """)
    Optional<Assignment> findWithCourseById(@Param("id") Long id);

    List<Assignment> findByLessonIdOrderByIdAsc(Long lessonId);

    @Query("""
            select a from Assignment a
            join fetch a.lesson l
            join fetch l.module m
            join fetch m.course c
            where c.id in (
                select e.course.id from Enrollment e where e.student.id = :studentId
            )
            order by c.id, m.orderIndex, l.orderIndex, a.id
            """)
    List<Assignment> findForStudent(@Param("studentId") Long studentId);

    long countByLessonModuleCourseId(Long courseId);

    @Query("select a.lesson.module.course.teacher.id from Assignment a where a.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
