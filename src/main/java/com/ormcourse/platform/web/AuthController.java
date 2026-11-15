package com.ormcourse.platform.web;

import com.ormcourse.platform.dto.auth.LoginRequest;
import com.ormcourse.platform.dto.auth.RegisterRequest;
import com.ormcourse.platform.dto.auth.TokenResponse;
import com.ormcourse.platform.dto.user.UserResponse;
import com.ormcourse.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Регистрация, вход и текущий пользователь")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Получить JWT по email и паролю")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Самостоятельная регистрация студента")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        return ResponseEntity.created(Locations.of("/api/users/{id}", created.id())).body(created);
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь по токену")
    public UserResponse me(Authentication authentication) {
        return authService.currentUser(Long.valueOf(authentication.getName()));
    }
}
