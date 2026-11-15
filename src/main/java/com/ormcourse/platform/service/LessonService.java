package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.course.LessonRequest;
import com.ormcourse.platform.dto.course.LessonResponse;
import com.ormcourse.platform.entity.CourseModule;
import com.ormcourse.platform.entity.Lesson;
import com.ormcourse.platform.entity.LessonCompletion;
import com.ormcourse.platform.entity.User;
import com.ormcourse.platform.exception.NotFoundException;
import com.ormcourse.platform.repository.LessonCompletionRepository;
import com.ormcourse.platform.repository.LessonRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonCompletionRepository completionRepository;
    private final ModuleService moduleService;
    private final UserLookup userLookup;
    private final EnrollmentGuard enrollmentGuard;
    private final Clock clock;

    @Transactional
    public LessonResponse addLesson(Long moduleId, LessonRequest request) {
        CourseModule module = moduleService.find(moduleId);
        int nextOrder = lessonRepository.findMaxOrderIndex(moduleId) + 1;
        Lesson lesson = CourseStructureFactory.buildLesson(request, nextOrder);
        module.addLesson(lesson);
        lessonRepository.save(lesson);
        log.info("Added lesson id={} to module id={}", lesson.getId(), moduleId);
        return LessonResponse.from(lesson);
    }

    public LessonResponse get(Long lessonId) {
        Lesson lesson = lessonRepository.findWithAssignmentsById(lessonId)
                .orElseThrow(() -> NotFoundException.of("Lesson", lessonId));
        return LessonResponse.from(lesson);
    }

    @Transactional
    public LessonResponse update(Long lessonId, LessonRequest request) {
        Lesson lesson = find(lessonId);
        CourseStructureFactory.applyLesson(lesson, request);
        return LessonResponse.from(lesson);
    }

    @Transactional
    public void delete(Long lessonId) {
        Lesson lesson = find(lessonId);
        lesson.getModule().getLessons().remove(lesson);
        log.info("Deleted lesson id={}", lessonId);
    }

    @Transactional
    public void markCompleted(Long lessonId, Long studentId) {
        Lesson lesson = find(lessonId);
        User student = userLookup.getStudent(studentId);
        enrollmentGuard.requireActiveEnrollment(studentId, lesson.getModule().getCourse().getId());

        if (completionRepository.existsByStudentIdAndLessonId(studentId, lessonId)) {
            return;
        }

        completionRepository.save(new LessonCompletion(student, lesson, clock.instant()));
        log.info("Student id={} completed lesson id={}", studentId, lessonId);
    }

    Lesson find(Long lessonId) {
        return lessonRepository.findWithCourseById(lessonId).orElseThrow(() -> NotFoundException.of("Lesson", lessonId));
    }
}
