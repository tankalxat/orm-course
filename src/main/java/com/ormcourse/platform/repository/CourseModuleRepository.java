package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.CourseModule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    @Query("""
            select distinct m from CourseModule m
            left join fetch m.lessons
            left join fetch m.quiz
            where m.course.id = :courseId
            order by m.orderIndex
            """)
    List<CourseModule> findStructureByCourseId(@Param("courseId") Long courseId);

    @Query("select m from CourseModule m join fetch m.course where m.id = :id")
    Optional<CourseModule> findWithCourseById(@Param("id") Long id);

    @Query("select coalesce(max(m.orderIndex), 0) from CourseModule m where m.course.id = :courseId")
    int findMaxOrderIndex(@Param("courseId") Long courseId);

    long countByCourseId(Long courseId);

    @Query("select m.course.teacher.id from CourseModule m where m.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
