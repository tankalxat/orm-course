package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Submission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @EntityGraph(attributePaths = {"assignment", "student"})
    Optional<Submission> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"assignment", "student"})
    List<Submission> findByAssignmentIdOrderBySubmittedAtAsc(Long assignmentId);

    @EntityGraph(attributePaths = {"assignment", "student"})
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);

    @Query("""
            select count(s) > 0 from Submission s
            where s.assignment.lesson.module.course.id = :courseId
            """)
    boolean existsByCourseId(@Param("courseId") Long courseId);

    @Query("select s.assignment.lesson.module.course.teacher.id from Submission s where s.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);

    @Query("select s.student.id from Submission s where s.id = :id")
    Optional<Long> findStudentIdById(@Param("id") Long id);
}
