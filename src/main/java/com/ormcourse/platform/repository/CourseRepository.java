package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Course;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    @Override
    @EntityGraph(attributePaths = {"category", "teacher"})
    Page<Course> findAll(Specification<Course> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "teacher", "tags"})
    Optional<Course> findWithDetailsById(Long id);

    @Query("select c from Course c left join fetch c.modules where c.id = :id")
    Optional<Course> findWithModulesById(@Param("id") Long id);

    boolean existsByTeacherId(Long teacherId);

    boolean existsByCategoryId(Long categoryId);

    @Query("select c.teacher.id from Course c where c.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
