package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.common.PageResponse;
import com.ormcourse.platform.dto.user.ProfileRequest;
import com.ormcourse.platform.dto.user.ProfileResponse;
import com.ormcourse.platform.dto.user.UserCreateRequest;
import com.ormcourse.platform.dto.user.UserResponse;
import com.ormcourse.platform.dto.user.UserUpdateRequest;
import com.ormcourse.platform.entity.Role;
import com.ormcourse.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Пользователи и профили")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Список пользователей с фильтром по роли")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) Role role,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return userService.list(role, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Пользователь по id")
    @PreAuthorize("@access.isSelfOrStaff(authentication, #id)")
    public UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    @Operation(summary = "Создать пользователя")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.create(request);
        return ResponseEntity.created(Locations.of("/api/users/{id}", created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя")
    @PreAuthorize("@access.isSelfOrAdmin(authentication, #id)")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя вместе с его записями, решениями и отзывами")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Профиль пользователя")
    public ProfileResponse getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Создать или обновить профиль пользователя")
    @PreAuthorize("@access.isSelfOrAdmin(authentication, #id)")
    public ProfileResponse upsertProfile(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        return userService.upsertProfile(id, request);
    }
}
