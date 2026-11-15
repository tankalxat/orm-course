package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.assignment.AssignmentRequest;
import com.ormcourse.platform.dto.assignment.AssignmentResponse;
import com.ormcourse.platform.dto.course.LessonRequest;
import com.ormcourse.platform.dto.course.LessonResponse;
import com.ormcourse.platform.service.AssignmentService;
import com.ormcourse.platform.service.LessonService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lessons")
@Tag(name = "Lessons", description = "Уроки, задания урока и прогресс")
public class LessonController {

    private final LessonService lessonService;
    private final AssignmentService assignmentService;

    @GetMapping("/{id}")
    @Operation(summary = "Урок с материалами и заданиями")
    public LessonResponse get(@PathVariable Long id) {
        return lessonService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить урок")
    @PreAuthorize("@access.canManageLesson(authentication, #id)")
    public LessonResponse update(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return lessonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить урок вместе с заданиями")
    @PreAuthorize("@access.canManageLesson(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "Задания урока")
    public List<AssignmentResponse> assignments(@PathVariable Long id) {
        return assignmentService.listByLesson(id);
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Создать задание к уроку")
    @PreAuthorize("@access.canManageLesson(authentication, #id)")
    public ResponseEntity<AssignmentResponse> createAssignment(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse created = assignmentService.create(id, request);
        return ResponseEntity.created(Locations.of("/api/assignments/{id}", created.id())).body(created);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Отметить урок пройденным студентом")
    @PreAuthorize("@access.isSelf(authentication, #userId)")
    public ResponseEntity<Void> complete(@PathVariable Long id, @RequestParam("user") Long userId) {
        lessonService.markCompleted(id, userId);
        return ResponseEntity.noContent().build();
    }
}
