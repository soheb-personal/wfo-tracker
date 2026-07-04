package com.wfotracker.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditEmployeeRequest(
        @NotBlank(message = "Employee name is required")
                @Size(max = 100, message = "Employee name must be less than 100 characters")
                String employeeName,
        @NotBlank(message = "Username is required")
                @Size(max = 50, message = "Username must be less than 50 characters")
                String username) {}
