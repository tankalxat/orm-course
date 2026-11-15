package com.ormcourse.platform.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 30) @Pattern(regexp = "^[+0-9() -]*$", message = "must contain only digits, spaces, +, -, ( )") String phone
) {
}
