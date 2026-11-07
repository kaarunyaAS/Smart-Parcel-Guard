package com.smartparcelguard.auth.dto;

import com.smartparcelguard.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}
