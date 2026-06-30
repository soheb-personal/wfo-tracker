package com.wfotracker.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "New password cannot be empty")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword,
        
        @NotBlank(message = "Confirm password cannot be empty")
        String confirmPassword
) {}
