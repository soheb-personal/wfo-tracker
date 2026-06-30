package com.wfotracker.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddEmployeeRequest(
        @NotBlank(message = "Employee name is required")
        @Size(max = 100, message = "Employee name must be less than 100 characters")
        String employeeName
) {}
