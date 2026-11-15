package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.common.PageResponse;
import com.ormcourse.platform.dto.course.CourseCreateRequest;
import com.ormcourse.platform.dto.course.CourseDetailsResponse;
import com.ormcourse.platform.dto.course.CourseSearchFilter;
import com.ormcourse.platform.dto.course.CourseSummaryResponse;
import com.ormcourse.platform.dto.course.CourseUpdateRequest;
import com.ormcourse.platform.dto.course.ModuleRequest;
import com.ormcourse.platform.dto.course.ModuleResponse;
import com.ormcourse.platform.security.AccessPolicy;
import com.ormcourse.platform.service.CourseService;
import com.ormcourse.platform.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
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
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Курсы, их структура и теги")
public class CourseController {

    private final CourseService courseService;
    private final ModuleService moduleService;
    private final AccessPolicy access;

    @GetMapping
    @Operation(summary = "Поиск курсов: categoryId, teacherId, status, tag, query (DRAFT — только своему преподавателю)")
    public PageResponse<CourseSummaryResponse> search(
            @ParameterObject CourseSearchFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication
    ) {
        return courseService.search(filter, access.visibleCourses(authentication), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Курс со структурой модулей и уроков")
    public CourseDetailsResponse get(
            @PathVariable Long id,
            @CurrentSecurityContext(expression = "authentication") Authentication authentication
    ) {
        courseService.requireVisible(id, access.visibleCourses(authentication));
        return courseService.getDetails(id);
    }

    @PostMapping
    @Operation(summary = "Создать курс, опционально сразу с модулями и уроками")
    @PreAuthorize("@access.canAssignTeacher(authentication, #request.teacherId())")
    public ResponseEntity<CourseDetailsResponse> create(@Valid @RequestBody CourseCreateRequest request) {
        CourseDetailsResponse created = courseService.create(request);
        return ResponseEntity.created(Locations.of("/api/courses/{id}", created.course().id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить курс: описание, категорию, преподавателя, статус")
    @PreAuthorize("@access.canManageCourse(authentication, #id) and @access.canAssignTeacher(authentication, #request.teacherId())")
    public CourseDetailsResponse update(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить курс без записанных студентов (каскадно со структурой)")
    @PreAuthorize("@access.canManageCourse(authentication, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/modules")
    @Operation(summary = "Добавить модуль в курс")
    @PreAuthorize("@access.canManageCourse(authentication, #id)")
    public ResponseEntity<ModuleResponse> addModule(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        ModuleResponse created = moduleService.addModule(id, request);
        return ResponseEntity.created(Locations.of("/api/modules/{id}", created.id())).body(created);
    }

    @PutMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Привязать тег к курсу")
    @PreAuthorize("@access.canManageCourse(authentication, #id)")
    public CourseSummaryResponse addTag(@PathVariable Long id, @PathVariable Long tagId) {
        return courseService.addTag(id, tagId);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "Отвязать тег от курса")
    @PreAuthorize("@access.canManageCourse(authentication, #id)")
    public CourseSummaryResponse removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return courseService.removeTag(id, tagId);
    }
}
