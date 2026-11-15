package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.course.LessonRequest;
import com.ormcourse.platform.dto.course.LessonResponse;
import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.dto.course.ModuleResponse;
import com.ormcourse.platform.dto.quiz.QuizRequest;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.service.LessonService;
import com.ormcourse.platform.service.ModuleService;
import com.ormcourse.platform.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/modules")
@Tag(name = "Modules", description = "Модули курса, уроки и тесты модуля")
public class ModuleController {

    private final ModuleService moduleService;
    private final LessonService lessonService;
    private final QuizService quizService;

    @GetMapping("/{id}")
    @Operation(summary = "Модуль с уроками")
    public ModuleResponse get(@PathVariable Long id) {
        return moduleService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить модуль")
    @PreAuthorize("@access.canManageModule(authentication, #id)")
    public ModuleResponse update(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        return moduleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить модуль вместе с уроками и тестом")
    @PreAuthorize("@access.canManageModule(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        moduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lessons")
    @Operation(summary = "Добавить урок в модуль")
    @PreAuthorize("@access.canManageModule(authentication, #id)")
    public ResponseEntity<LessonResponse> addLesson(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        LessonResponse created = lessonService.addLesson(id, request);
        return ResponseEntity.created(Locations.of("/api/lessons/{id}", created.id())).body(created);
    }

    @PostMapping("/{id}/quiz")
    @Operation(summary = "Создать тест модуля с вопросами")
    @PreAuthorize("@access.canManageModule(authentication, #id)")
    public ResponseEntity<QuizResponse> createQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        QuizResponse created = quizService.create(id, request);
        return ResponseEntity.created(Locations.of("/api/quizzes/{id}", created.id())).body(created);
    }

    @GetMapping("/{id}/quiz")
    @Operation(summary = "Тест модуля")
    @PreAuthorize("!#includeAnswers or @access.canManageModule(authentication, #id)")
    public QuizResponse getQuiz(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean includeAnswers) {
        return quizService.getByModule(id, includeAnswers);
    }
}
