package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.CourseReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    @EntityGraph(attributePaths = {"course", "student"})
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    @Query("select r.student.id from CourseReview r where r.id = :id")
    Optional<Long> findStudentIdById(@Param("id") Long id);
}
