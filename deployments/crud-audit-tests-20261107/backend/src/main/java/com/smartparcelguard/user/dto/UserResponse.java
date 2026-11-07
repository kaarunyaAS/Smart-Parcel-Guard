package com.smartparcelguard.user.dto;

import com.smartparcelguard.user.UserRole;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        UserRole role,
        boolean enabled
) {
}
