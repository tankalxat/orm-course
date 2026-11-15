package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.assignment.AssignmentRequest;
import com.ormcourse.platform.dto.assignment.AssignmentResponse;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.dto.assignment.SubmitRequest;
import com.ormcourse.platform.service.AssignmentService;
import com.ormcourse.platform.service.SubmissionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assignments")
@Tag(name = "Assignments", description = "Задания и отправка решений")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @GetMapping("/{id}")
    @Operation(summary = "Задание по id")
    public AssignmentResponse get(@PathVariable Long id) {
        return assignmentService.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить задание")
    @PreAuthorize("@access.canManageAssignment(authentication, #id)")
    public AssignmentResponse update(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить задание вместе с решениями")
    @PreAuthorize("@access.canManageAssignment(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Отправить решение задания (одно на студента)")
    @PreAuthorize("@access.isSelf(authentication, #request.studentId())")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long id, @Valid @RequestBody SubmitRequest request) {
        SubmissionResponse created = submissionService.submit(id, request);
        return ResponseEntity.created(Locations.of("/api/submissions/{id}", created.id())).body(created);
    }

    @GetMapping("/{id}/submissions")
    @Operation(summary = "Все решения по заданию")
    @PreAuthorize("@access.canManageAssignment(authentication, #id)")
    public List<SubmissionResponse> submissions(@PathVariable Long id) {
        return submissionService.listByAssignment(id);
    }
}
