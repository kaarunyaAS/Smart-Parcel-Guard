package com.smartparcelguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "may contain letters, numbers, dots, underscores, and hyphens")
        String username,
        @NotBlank @Email @Size(max = 160)
        String email,
        @NotBlank @Size(min = 8, max = 72)
        String password,
        @NotBlank @Size(max = 160)
        String fullName
) {
}
