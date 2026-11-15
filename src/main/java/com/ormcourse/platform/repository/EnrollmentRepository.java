package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.entity.EnrollmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = {"student", "course"})
    Optional<Enrollment> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"student", "course"})
    List<Enrollment> findByCourseIdOrderByEnrolledAtAsc(Long courseId);

    @EntityGraph(attributePaths = {"student", "course", "course.category", "course.teacher"})
    List<Enrollment> findByStudentIdOrderByEnrolledAtAsc(Long studentId);

    @Query("select e.student.id from Enrollment e where e.course.id = :courseId and e.status = :status")
    List<Long> findStudentIdsByCourseIdAndStatus(@Param("courseId") Long courseId, @Param("status") EnrollmentStatus status);

    boolean existsByCourseId(Long courseId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    @Query("select e.course.teacher.id from Enrollment e where e.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
