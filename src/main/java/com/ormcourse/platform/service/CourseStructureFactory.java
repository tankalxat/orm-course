package com.ormcourse.platform.service;

import com.ormcourse.platform.dto.course.LessonRequest;
import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.dto.quiz.AnswerOptionRequest;
import com.ormcourse.platform.dto.quiz.QuestionRequest;
import com.ormcourse.platform.entity.AnswerOption;
import com.ormcourse.platform.entity.CourseModule;
import com.ormcourse.platform.entity.Lesson;
import com.ormcourse.platform.entity.Question;
import java.util.List;

public final class CourseStructureFactory {

    private CourseStructureFactory() {
    }

    public static CourseModule buildModule(ModuleRequest request, int defaultOrderIndex) {
        CourseModule module = new CourseModule(
                request.title(),
                request.description(),
                orderOrDefault(request.orderIndex(), defaultOrderIndex)
        );
        List<LessonRequest> lessons = request.lessonsOrEmpty();

        for (int i = 0; i < lessons.size(); i++) {
            module.addLesson(buildLesson(lessons.get(i), i + 1));
        }

        return module;
    }

    public static Lesson buildLesson(LessonRequest request, int defaultOrderIndex) {
        Lesson lesson = new Lesson(request.title(), request.content(), orderOrDefault(request.orderIndex(), defaultOrderIndex));
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationMinutes(request.durationMinutes());
        return lesson;
    }

    public static Question buildQuestion(QuestionRequest request, int orderIndex) {
        Question question = new Question(request.text(), request.type(), orderIndex);

        for (AnswerOptionRequest option : request.options()) {
            question.addOption(new AnswerOption(option.text(), option.correct()));
        }

        return question;
    }

    public static void applyLesson(Lesson lesson, LessonRequest request) {
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setDurationMinutes(request.durationMinutes());

        if (request.orderIndex() != null) {
            lesson.setOrderIndex(request.orderIndex());
        }
    }

    private static int orderOrDefault(Integer requested, int defaultValue) {
        return requested == null ? defaultValue : requested;
    }
}
