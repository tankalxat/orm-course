package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.enrollment.EnrollmentResponse;
import com.ormcourse.platform.dto.enrollment.EnrollmentStatusRequest;
import com.ormcourse.platform.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Запись студентов на курсы")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enroll")
    @Operation(summary = "Записать студента на курс")
    @PreAuthorize("@access.isSelfOrAdmin(authentication, #userId)")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long courseId, @RequestParam("user") Long userId) {
        EnrollmentResponse created = enrollmentService.enroll(courseId, userId);
        return ResponseEntity.created(Locations.of("/api/enrollments/{id}", created.id())).body(created);
    }

    @DeleteMapping("/courses/{courseId}/enroll")
    @Operation(summary = "Отписать студента от курса")
    @PreAuthorize("@access.isSelf(authentication, #userId) or @access.canManageCourse(authentication, #courseId)")
    public ResponseEntity<Void> unenroll(@PathVariable Long courseId, @RequestParam("user") Long userId) {
        enrollmentService.unenroll(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses/{courseId}/students")
    @Operation(summary = "Студенты, записанные на курс")
    @PreAuthorize("@access.canManageCourse(authentication, #courseId)")
    public List<EnrollmentResponse> students(@PathVariable Long courseId) {
        return enrollmentService.listStudents(courseId);
    }

    @PatchMapping("/enrollments/{id}")
    @Operation(summary = "Изменить статус записи (ACTIVE/COMPLETED)")
    @PreAuthorize("@access.canManageEnrollment(authentication, #id)")
    public EnrollmentResponse changeStatus(@PathVariable Long id, @Valid @RequestBody EnrollmentStatusRequest request) {
        return enrollmentService.changeStatus(id, request.status());
    }
}
