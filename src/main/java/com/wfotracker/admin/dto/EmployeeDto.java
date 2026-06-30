package com.wfotracker.admin.dto;

import com.wfotracker.domain.entity.User;

public record EmployeeDto(
        Long id,
        String fullName,
        String username,
        boolean active
) {
    public static EmployeeDto from(User user) {
        return new EmployeeDto(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.isActive()
        );
    }
}
