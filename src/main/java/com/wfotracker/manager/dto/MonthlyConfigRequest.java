package com.wfotracker.manager.dto;

import jakarta.validation.constraints.Min;

public record MonthlyConfigRequest(
        @Min(0) int leaves,
        @Min(0) int publicHolidays,
        @Min(0) int exceptionDays,
        @Min(0) int manualCheckins,
        int month,
        int year
) {}
