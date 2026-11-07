package com.smartparcelguard.user.dto;

import com.smartparcelguard.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z0-9._-]+$")
        String username,
        @NotBlank @Email @Size(max = 160)
        String email,
        @NotBlank @Size(min = 8, max = 72)
        String password,
        @NotBlank @Size(max = 160)
        String fullName,
        @NotNull
        UserRole role
) {
}
