package com.ormcourse.platform.repository;

import com.ormcourse.platform.entity.Question;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            select distinct q from Question q
            left join fetch q.options
            where q.quiz.id = :quizId
            order by q.orderIndex
            """)
    List<Question> findWithOptionsByQuizId(@Param("quizId") Long quizId);

    @Query("select q from Question q join fetch q.quiz where q.id = :id")
    Optional<Question> findWithQuizById(@Param("id") Long id);

    @Query("select coalesce(max(q.orderIndex), 0) from Question q where q.quiz.id = :quizId")
    int findMaxOrderIndex(@Param("quizId") Long quizId);

    @Query("select q.quiz.module.course.teacher.id from Question q where q.id = :id")
    Optional<Long> findTeacherIdById(@Param("id") Long id);
}
