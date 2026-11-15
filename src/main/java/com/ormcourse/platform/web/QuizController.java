package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.quiz.QuestionRequest;
import com.ormcourse.platform.dto.quiz.QuestionResponse;
import com.ormcourse.platform.dto.quiz.QuizResponse;
import com.ormcourse.platform.dto.quiz.QuizSubmissionResponse;
import com.ormcourse.platform.dto.quiz.TakeQuizRequest;
import com.ormcourse.platform.service.QuizAttemptService;
import com.ormcourse.platform.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Тесты, вопросы и прохождение")
public class QuizController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;

    @GetMapping("/quizzes/{id}")
    @Operation(summary = "Тест с вопросами; includeAnswers=true показывает правильные ответы")
    @PreAuthorize("!#includeAnswers or @access.canManageQuiz(authentication, #id)")
    public QuizResponse get(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean includeAnswers) {
        return quizService.get(id, includeAnswers);
    }

    @DeleteMapping("/quizzes/{id}")
    @Operation(summary = "Удалить тест вместе с вопросами и результатами")
    @PreAuthorize("@access.canManageQuiz(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/quizzes/{id}/questions")
    @Operation(summary = "Добавить вопрос (только пока нет попыток)")
    @PreAuthorize("@access.canManageQuiz(authentication, #id)")
    public ResponseEntity<QuestionResponse> addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        QuestionResponse created = quizService.addQuestion(id, request);
        return ResponseEntity.created(Locations.of("/api/quizzes/{id}", id)).body(created);
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Удалить вопрос (только пока нет попыток)")
    @PreAuthorize("@access.canManageQuestion(authentication, #id)")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/quizzes/{id}/take")
    @Operation(summary = "Пройти тест: answers = {questionId: [optionId, ...]}")
    @PreAuthorize("@access.isSelf(authentication, #request.studentId())")
    public ResponseEntity<QuizSubmissionResponse> take(@PathVariable Long id, @Valid @RequestBody TakeQuizRequest request) {
        QuizSubmissionResponse created = quizAttemptService.takeQuiz(id, request);
        return ResponseEntity.created(Locations.of("/api/quizzes/{id}/submissions", id)).body(created);
    }

    @GetMapping("/quizzes/{id}/submissions")
    @Operation(summary = "Результаты теста")
    @PreAuthorize("@access.canManageQuiz(authentication, #id)")
    public List<QuizSubmissionResponse> submissions(@PathVariable Long id) {
        return quizAttemptService.listByQuiz(id);
    }

    @GetMapping("/courses/{courseId}/quiz-submissions")
    @Operation(summary = "Результаты всех тестов курса")
    @PreAuthorize("@access.canManageCourse(authentication, #courseId)")
    public List<QuizSubmissionResponse> courseSubmissions(@PathVariable Long courseId) {
        return quizAttemptService.listByCourse(courseId);
    }
}
