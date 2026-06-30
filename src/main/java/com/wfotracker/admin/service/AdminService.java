package com.wfotracker.admin.service;

import com.wfotracker.admin.dto.CreateTeamRequest;
import com.wfotracker.admin.dto.EditTeamRequest;
import com.wfotracker.admin.dto.TeamDto;
import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.TeamRepository;
import com.wfotracker.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::mapToTeamDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByTeamName(request.teamName())) {
            throw new IllegalArgumentException("Team name already exists");
        }

        Team team = new Team();
        team.setTeamName(request.teamName());
        team = teamRepository.save(team);

        User manager = new User();
        manager.setFullName(request.managerName());
        
        String[] nameParts = request.managerName().trim().split("\\s+");
        String firstName = nameParts[0].toLowerCase();
        String username = getUniqueUsername(firstName);
        manager.setUsername(username);

        String surnameInitial = nameParts.length > 1 ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase() : "";
        String defaultPassword = firstName + surnameInitial + "@123";
        manager.setPassword(passwordEncoder.encode(defaultPassword));
        manager.setRole(Role.MANAGER);
        manager.setTeam(team);
        
        userRepository.save(manager);
    }

    @Transactional(readOnly = true)
    public EditTeamRequest getTeamForEdit(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        User manager = getManagerForTeam(teamId);
        
        return new EditTeamRequest(
                team.getTeamName(),
                manager != null ? manager.getFullName() : "",
                manager != null ? manager.getUsername() : ""
        );
    }

    @Transactional
    public void editTeam(Long teamId, EditTeamRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        if (!team.getTeamName().equals(request.teamName()) && teamRepository.existsByTeamName(request.teamName())) {
            throw new IllegalArgumentException("Team name already exists");
        }
        
        team.setTeamName(request.teamName());
        
        User manager = getManagerForTeam(teamId);
        if (manager != null) {
            manager.setFullName(request.managerName());
            
            if (!manager.getUsername().equals(request.managerUsername())) {
                if (userRepository.existsByUsername(request.managerUsername())) {
                    throw new IllegalArgumentException("Username already exists");
                }
                manager.setUsername(request.managerUsername());
            }
        }
    }

    @Transactional
    public void deactivateTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        
        team.setActive(false);
        
        List<User> teamMembers = userRepository.findByTeamId(teamId);
        for (User user : teamMembers) {
            user.setActive(false);
        }
    }

    @Transactional
    public void resetManagerPassword(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
                
        if (manager.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException("User is not a manager");
        }

        String[] nameParts = manager.getFullName().trim().split("\\s+");
        String firstName = nameParts[0].toLowerCase();
        String surnameInitial = nameParts.length > 1 ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase() : "";
        String defaultPassword = firstName + surnameInitial + "@123";
        
        manager.setPassword(passwordEncoder.encode(defaultPassword));
        manager.setPasswordChanged(false);
    }

    private TeamDto mapToTeamDto(Team team) {
        User manager = getManagerForTeam(team.getId());
        List<User> employees = userRepository.findByTeamIdAndRole(team.getId(), Role.EMPLOYEE);
        return TeamDto.from(team, manager, employees);
    }

    private User getManagerForTeam(Long teamId) {
        return userRepository.findByTeamIdAndRole(teamId, Role.MANAGER)
                .stream().findFirst().orElse(null);
    }

    private String getUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        return username;
    }
}
