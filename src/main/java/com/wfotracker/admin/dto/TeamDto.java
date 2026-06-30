package com.wfotracker.admin.dto;

import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.User;

import java.util.List;

public record TeamDto(
        Long id,
        String teamName,
        Long managerId,
        String managerName,
        String managerUsername,
        boolean active,
        List<EmployeeDto> employees
) {
    public static TeamDto from(Team team, User manager, List<User> employees) {
        return new TeamDto(
                team.getId(),
                team.getTeamName(),
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null,
                manager != null ? manager.getUsername() : null,
                team.isActive(),
                employees.stream().map(EmployeeDto::from).toList()
        );
    }
}
