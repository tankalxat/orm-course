package com.ormcourse.platform.dto.auth;

import com.ormcourse.platform.dto.user.UserResponse;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
