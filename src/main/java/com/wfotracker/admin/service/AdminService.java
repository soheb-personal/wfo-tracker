package com.wfotracker.admin.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.admin.dto.CreateTeamRequest;
import com.wfotracker.admin.dto.EditTeamRequest;
import com.wfotracker.admin.dto.TeamDto;
import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.EmployeeMembership;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.EmployeeMembershipRepository;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.TeamRepository;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String MSG_TEAM_NOT_FOUND = "Team not found";
    private static final String MSG_MANAGER_NOT_FOUND = "Manager not found";
    private static final String MSG_ROLE_NOT_FOUND = "ROLE_MANAGER role not found";
    private static final String MSG_TEAM_NAME_EXISTS = "Team name already exists";
    private static final String MSG_MANAGER_DAS_EXISTS = "Manager DAS ID already exists";
    private static final String ROLE_MANAGER_NAME = "ROLE_MANAGER";

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final EmployeeMembershipRepository employeeMembershipRepository;

    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream().map(this::mapToTeamDto).toList();
    }

    @Transactional
    public void createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByTeamName(request.teamName())) {
            throw new IllegalArgumentException(MSG_TEAM_NAME_EXISTS);
        }

        com.wfotracker.domain.entity.Role managerRole = roleRepository
                .findByName(ROLE_MANAGER_NAME)
                .orElseThrow(() -> new IllegalStateException(MSG_ROLE_NOT_FOUND));

        Team team = new Team();
        team.setTeamName(request.teamName());
        team = teamRepository.save(team);

        User managerUser;
        Optional<User> existingUserOpt = userRepository.findByUsername(request.managerDasId());

        if (existingUserOpt.isPresent()) {
            managerUser = existingUserOpt.get();
            if (teamManagerRepository.existsByManagerIdAndActiveTrue(managerUser.getId())) {
                throw new IllegalArgumentException("This user is already an active manager of another team");
            }
            if (!managerUser.getRoles().contains(managerRole)) {
                managerUser.getRoles().add(managerRole);
            }
            managerUser.setFullName(request.managerName());
            managerUser = userRepository.save(managerUser);
        } else {
            managerUser = new User();
            managerUser.setFullName(request.managerName());
            managerUser.setUsername(request.managerDasId());

            String[] nameParts = request.managerName().trim().split("\\s+");
            String firstName = nameParts[0].toLowerCase();
            String surnameInitial = nameParts.length > 1
                    ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase()
                    : "";
            String defaultPassword = firstName + surnameInitial + "@123";
            managerUser.setPassword(passwordEncoder.encode(defaultPassword));
            managerUser.getRoles().add(managerRole);
            managerUser = userRepository.save(managerUser);
        }

        TeamManager teamManager = new TeamManager();
        teamManager.setTeam(team);
        teamManager.setManager(managerUser);
        teamManagerRepository.save(teamManager);
    }

    @Transactional(readOnly = true)
    public EditTeamRequest getTeamForEdit(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException(MSG_TEAM_NOT_FOUND));
        User manager = getManagerForTeam(teamId);

        return new EditTeamRequest(
                team.getTeamName(),
                manager != null ? manager.getFullName() : "",
                manager != null ? manager.getUsername() : "");
    }

    @Transactional
    public void editTeam(Long teamId, EditTeamRequest request) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException(MSG_TEAM_NOT_FOUND));

        if (!team.getTeamName().equals(request.teamName()) && teamRepository.existsByTeamName(request.teamName())) {
            throw new IllegalArgumentException(MSG_TEAM_NAME_EXISTS);
        }

        team.setTeamName(request.teamName());

        User manager = getManagerForTeam(teamId);
        if (manager != null) {
            manager.setFullName(request.managerName());

            if (!manager.getUsername().equals(request.managerUsername())) {
                if (userRepository.existsByUsername(request.managerUsername())) {
                    throw new IllegalArgumentException(MSG_MANAGER_DAS_EXISTS);
                }
                manager.setUsername(request.managerUsername());
            }
        }
    }

    @Transactional
    public void deactivateTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException(MSG_TEAM_NOT_FOUND));

        team.setActive(false);

        // Deactivate active team manager relationship
        teamManagerRepository.findByTeamIdAndActiveTrue(teamId).ifPresent(tm -> tm.setActive(false));

        // Deactivate active employee memberships under this team
        List<EmployeeMembership> activeMemberships = employeeMembershipRepository.findByTeamIdAndActiveTrue(teamId);
        for (EmployeeMembership membership : activeMemberships) {
            membership.setActive(false);
        }
    }

    @Transactional
    public void resetManagerPassword(Long managerId) {
        User manager = userRepository
                .findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_MANAGER_NOT_FOUND));

        boolean isManager =
                manager.getRoles().stream().anyMatch(r -> r.getName().equals(ROLE_MANAGER_NAME));
        if (!isManager) {
            throw new IllegalArgumentException("User is not a manager");
        }

        String[] nameParts = manager.getFullName().trim().split("\\s+");
        String firstName = nameParts[0].toLowerCase();
        String surnameInitial = nameParts.length > 1
                ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase()
                : "";
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
        return userRepository.findByTeamIdAndRole(teamId, Role.MANAGER).stream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException(MSG_TEAM_NOT_FOUND));

        if (team.isActive()) {
            throw new IllegalArgumentException("Cannot delete an active team. Deactivate it first.");
        }

        teamRepository.delete(team);
    }
}
