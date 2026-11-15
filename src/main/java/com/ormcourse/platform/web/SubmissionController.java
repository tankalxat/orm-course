package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.assignment.GradeRequest;
import com.ormcourse.platform.dto.assignment.SubmissionResponse;
import com.ormcourse.platform.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/submissions")
@Tag(name = "Submissions", description = "Проверка и оценивание решений")
public class SubmissionController {

    private final SubmissionService submissionService;

    @GetMapping("/{id}")
    @Operation(summary = "Решение по id")
    @PreAuthorize("@access.canViewSubmission(authentication, #id)")
    public SubmissionResponse get(@PathVariable Long id) {
        return submissionService.get(id);
    }

    @PutMapping("/{id}/grade")
    @Operation(summary = "Выставить оценку и комментарий")
    @PreAuthorize("@access.canGradeSubmission(authentication, #id)")
    public SubmissionResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return submissionService.grade(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Отозвать непроверенное решение")
    @PreAuthorize("@access.ownsSubmission(authentication, #id) or hasRole('ADMIN')")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        submissionService.withdraw(id);
        return ResponseEntity.noContent().build();
    }
}
