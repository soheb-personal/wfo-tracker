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
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.TeamRepository;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String MSG_TEAM_NOT_FOUND = "Team not found";

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final org.springframework.security.core.session.SessionRegistry sessionRegistry;

    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream().map(this::mapToTeamDto).toList();
    }

    @Transactional
    public void createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByTeamName(request.teamName())) {
            throw new IllegalArgumentException("Team name already exists");
        }

        com.wfotracker.domain.entity.Role managerRole = roleRepository
                .findByName("ROLE_MANAGER")
                .orElseThrow(() -> new IllegalStateException("ROLE_MANAGER role not found"));

        Team team = new Team();
        team.setTeamName(request.teamName());
        team = teamRepository.save(team);

        User managerUser;
        Optional<User> existingUserOpt = userRepository.findByUsername(request.managerDasId());

        if (existingUserOpt.isPresent()) {
            managerUser = existingUserOpt.get();
            // Validate that the manager is not already actively managing another team
            if (teamManagerRepository.existsByManagerIdAndActiveTrue(managerUser.getId())) {
                throw new IllegalArgumentException("This user is already an active manager of another team");
            }
            // Add ROLE_MANAGER role if they don't have it
            if (!managerUser.getRoles().contains(managerRole)) {
                managerUser.getRoles().add(managerRole);
            }
            // Update full name to the latest input
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
            throw new IllegalArgumentException("Team name already exists");
        }

        team.setTeamName(request.teamName());

        User manager = getManagerForTeam(teamId);
        if (manager != null) {
            manager.setFullName(request.managerName());

            if (!manager.getUsername().equals(request.managerUsername())) {
                if (userRepository.existsByUsername(request.managerUsername())) {
                    throw new IllegalArgumentException("Manager DAS ID already exists");
                }
                manager.setUsername(request.managerUsername());
            }
        }
    }

    @Transactional
    public void deactivateTeam(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException(MSG_TEAM_NOT_FOUND));

        team.setActive(false);

        // Deactivate manager and expire their sessions
        User manager = getManagerForTeam(teamId);
        if (manager != null) {
            manager.setActive(false);
            expireUserSessions(manager.getId());
        }

        // Deactivate the team-manager mapping
        teamManagerRepository.findByTeamIdAndActiveTrue(teamId).ifPresent(tm -> tm.setActive(false));

        // Deactivate employees and expire their sessions
        List<User> teamMembers = userRepository.findByTeamId(teamId);
        for (User user : teamMembers) {
            user.setActive(false);
            expireUserSessions(user.getId());
        }
    }

    private void expireUserSessions(Long userId) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof com.wfotracker.common.security.CustomUserDetails userDetails
                    && userDetails.getId().equals(userId)) {
                List<org.springframework.security.core.session.SessionInformation> sessions =
                        sessionRegistry.getAllSessions(principal, false);
                for (org.springframework.security.core.session.SessionInformation session : sessions) {
                    session.expireNow();
                }
            }
        }
    }

    @Transactional
    public void resetManagerPassword(Long managerId) {
        User manager =
                userRepository.findById(managerId).orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        boolean isManager =
                manager.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_MANAGER"));
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
}
