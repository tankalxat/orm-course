package com.ormcourse.platform.repository;

import com.ormcourse.platform.dto.analytics.CoursePopularity;
import com.ormcourse.platform.dto.analytics.LeaderboardEntry;
import com.ormcourse.platform.entity.Enrollment;
import com.ormcourse.platform.repository.projection.CourseCount;
import com.ormcourse.platform.repository.projection.CourseScore;
import com.ormcourse.platform.repository.projection.QuizStats;
import com.ormcourse.platform.repository.projection.ReviewStats;
import com.ormcourse.platform.repository.projection.SubmissionStats;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AnalyticsRepository extends Repository<Enrollment, Long> {

    @Query("""
            select new com.ormcourse.platform.repository.projection.ReviewStats(count(r), avg(r.rating))
            from CourseReview r
            where r.course.id = :courseId
            """)
    ReviewStats reviewStats(@Param("courseId") Long courseId);

    @Query("""
            select new com.ormcourse.platform.repository.projection.SubmissionStats(
                count(s),
                count(s.score),
                avg(s.score * 100.0 / a.maxScore)
            )
            from Submission s
            join s.assignment a
            where a.lesson.module.course.id = :courseId
            """)
    SubmissionStats submissionStats(@Param("courseId") Long courseId);

    @Query("""
            select new com.ormcourse.platform.repository.projection.QuizStats(
                count(qs),
                avg(case when qs.passed = true then 100.0 else 0.0 end)
            )
            from QuizSubmission qs
            where qs.quiz.module.course.id = :courseId
            """)
    QuizStats quizStats(@Param("courseId") Long courseId);

    @Query("""
            select new com.ormcourse.platform.repository.projection.CourseCount(m.course.id, count(l))
            from Lesson l
            join l.module m
            where m.course.id in :courseIds
            group by m.course.id
            """)
    List<CourseCount> lessonsPerCourse(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            select new com.ormcourse.platform.repository.projection.CourseCount(m.course.id, count(lc))
            from LessonCompletion lc
            join lc.lesson l
            join l.module m
            where lc.student.id = :studentId
            group by m.course.id
            """)
    List<CourseCount> completedLessonsPerCourse(@Param("studentId") Long studentId);

    @Query("""
            select new com.ormcourse.platform.repository.projection.CourseCount(m.course.id, count(a))
            from Assignment a
            join a.lesson l
            join l.module m
            where m.course.id in :courseIds
            group by m.course.id
            """)
    List<CourseCount> assignmentsPerCourse(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            select new com.ormcourse.platform.repository.projection.CourseScore(
                m.course.id,
                count(s),
                avg(s.score * 100.0 / a.maxScore)
            )
            from Submission s
            join s.assignment a
            join a.lesson l
            join l.module m
            where s.student.id = :studentId
            group by m.course.id
            """)
    List<CourseScore> submissionsPerCourse(@Param("studentId") Long studentId);

    @Query("""
            select new com.ormcourse.platform.repository.projection.CourseCount(m.course.id, count(q))
            from Quiz q
            join q.module m
            where m.course.id in :courseIds
            group by m.course.id
            """)
    List<CourseCount> quizzesPerCourse(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            select new com.ormcourse.platform.repository.projection.CourseCount(m.course.id, count(distinct q.id))
            from QuizSubmission qs
            join qs.quiz q
            join q.module m
            where qs.student.id = :studentId and qs.passed = true
            group by m.course.id
            """)
    List<CourseCount> passedQuizzesPerCourse(@Param("studentId") Long studentId);

    @Query("""
            select new com.ormcourse.platform.dto.analytics.LeaderboardEntry(st.id, st.name, sum(s.score), count(s))
            from Submission s
            join s.student st
            where s.assignment.lesson.module.course.id = :courseId and s.score is not null
            group by st.id, st.name
            order by sum(s.score) desc, st.name asc
            """)
    List<LeaderboardEntry> leaderboard(@Param("courseId") Long courseId, Pageable pageable);

    @Query("""
            select new com.ormcourse.platform.dto.analytics.CoursePopularity(
                c.id,
                c.title,
                count(e.id),
                (select avg(r.rating) from CourseReview r where r.course.id = c.id)
            )
            from Course c
            left join c.enrollments e
            where c.status <> com.ormcourse.platform.entity.CourseStatus.DRAFT
            group by c.id, c.title
            order by count(e.id) desc, c.title asc
            """)
    List<CoursePopularity> mostPopularCourses(Pageable pageable);
}
