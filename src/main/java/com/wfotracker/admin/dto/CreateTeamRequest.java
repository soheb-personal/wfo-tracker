package com.wfotracker.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank(message = "Team name is required")
                @Size(max = 100, message = "Team name must be less than 100 characters")
                String teamName,
        @NotBlank(message = "Manager name is required")
                @Size(max = 100, message = "Manager name must be less than 100 characters")
                String managerName,
        @NotBlank(message = "Manager DAS ID is required")
                @Size(max = 10, message = "Manager DAS ID length must be less than or equal to 10")
                @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Manager DAS ID must be alphanumeric only")
                String managerDasId) {}
