package com.wfotracker.manager.dto;

public record EmployeeComplianceDto(
        Long employeeId,
        String employeeName,
        String username,
        boolean active,
        int workingDays,
        int leaves,
        int publicHolidays,
        int exceptionDays,
        int manualCheckins,
        int requiredOfficeDays,
        int actualOfficeDaysVisited,
        int remainingOfficeDays,
        int compliancePercentage) {}
