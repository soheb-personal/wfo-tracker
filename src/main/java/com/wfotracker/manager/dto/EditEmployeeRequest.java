package com.wfotracker.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EditEmployeeRequest(
        @NotBlank(message = "Employee name is required")
                @Size(max = 100, message = "Employee name must be less than 100 characters")
                String employeeName,
        @NotBlank(message = "Employee DAS ID is required")
                @Size(max = 10, message = "Employee DAS ID length must be less than or equal to 10")
                @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Employee DAS ID must be alphanumeric only")
                String username) {}
