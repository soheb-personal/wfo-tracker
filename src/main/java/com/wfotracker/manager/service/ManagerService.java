package com.wfotracker.manager.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.Attendance;
import com.wfotracker.domain.entity.EmployeeMembership;
import com.wfotracker.domain.entity.Group;
import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.EmployeeMembershipRepository;
import com.wfotracker.domain.repository.GroupRepository;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.UserRepository;
import com.wfotracker.manager.dto.AddEmployeeRequest;
import com.wfotracker.manager.dto.EditEmployeeRequest;
import com.wfotracker.manager.dto.MonthlyConfigRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private static final String MANAGER_UNASSIGNED = "Manager is not currently assigned to any team";

    private final UserRepository userRepository;
    private final MonthlyConfigurationRepository monthlyConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final EmployeeMembershipRepository employeeMembershipRepository;
    private final GroupRepository groupRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public List<User> getEmployeesForManager(Long managerId) {
        return userRepository.findByManagerIdAndRoleAndActiveTrue(managerId, Role.EMPLOYEE);
    }

    @Transactional(readOnly = true)
    public List<EmployeeMembership> getMembershipsForManager(Long managerId, Long groupId) {
        List<EmployeeMembership> memberships = employeeMembershipRepository.findByManagerId(managerId);

        if (groupId != null) {
            if (groupId == -1L) {
                memberships =
                        memberships.stream().filter(m -> m.getGroup() == null).toList();
            } else {
                memberships = memberships.stream()
                        .filter(m ->
                                m.getGroup() != null && m.getGroup().getId().equals(groupId))
                        .toList();
            }
        }
        return memberships;
    }

    @Transactional(readOnly = true)
    public List<Group> getGroupsForManager(Long managerId) {
        TeamManager teamManager = teamManagerRepository
                .findByManagerIdAndActiveTrue(managerId)
                .orElseThrow(() -> new IllegalArgumentException(MANAGER_UNASSIGNED));
        return groupRepository.findByTeamIdAndActiveTrue(teamManager.getTeam().getId());
    }

    @Transactional
    public void createGroup(Long managerId, String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }
        TeamManager teamManager = teamManagerRepository
                .findByManagerIdAndActiveTrue(managerId)
                .orElseThrow(() -> new IllegalArgumentException(MANAGER_UNASSIGNED));
        Team team = teamManager.getTeam();

        if (groupRepository.existsByTeamIdAndGroupNameAndActiveTrue(team.getId(), groupName.trim())) {
            throw new IllegalArgumentException("Group name already exists under this team");
        }

        Group group = new Group();
        group.setTeam(team);
        group.setGroupName(groupName.trim());
        group.setActive(true);
        groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long managerId, Long groupId) {
        TeamManager teamManager = teamManagerRepository
                .findByManagerIdAndActiveTrue(managerId)
                .orElseThrow(() -> new IllegalArgumentException(MANAGER_UNASSIGNED));
        Team team = teamManager.getTeam();

        Group group = groupRepository
                .findById(groupId)
                .filter(g -> g.isActive() && g.getTeam().getId().equals(team.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Group not found or unauthorized"));

        // Move all active employees inside this group under this team to the DEFAULT group (set group = null)
        List<EmployeeMembership> membershipsInGroup = employeeMembershipRepository.findByGroupIdAndActiveTrue(groupId);
        for (EmployeeMembership m : membershipsInGroup) {
            m.setGroup(null);
        }
        employeeMembershipRepository.saveAll(membershipsInGroup);

        groupRepository.delete(group);
    }

    @Transactional
    public void addEmployee(Long managerId, AddEmployeeRequest request) {
        User manager =
                userRepository.findById(managerId).orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        TeamManager teamManager = teamManagerRepository
                .findByManagerIdAndActiveTrue(managerId)
                .orElseThrow(() -> new IllegalArgumentException(MANAGER_UNASSIGNED));
        Team team = teamManager.getTeam();

        com.wfotracker.domain.entity.Role employeeRole = roleRepository
                .findByName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException("ROLE_EMPLOYEE role not found"));

        Group group = null;
        if (request.groupId() != null) {
            group = groupRepository
                    .findById(request.groupId())
                    .filter(g -> g.isActive() && g.getTeam().getId().equals(team.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid group selected"));
        }

        User employeeUser;
        Optional<User> existingUserOpt = userRepository.findByUsername(request.employeeDasId());

        if (existingUserOpt.isPresent()) {
            employeeUser = existingUserOpt.get();
            if (employeeMembershipRepository
                    .findByEmployeeIdAndActiveTrue(employeeUser.getId())
                    .isPresent()) {
                throw new IllegalArgumentException("Employee is already active in another team");
            }
            if (!employeeUser.getRoles().contains(employeeRole)) {
                employeeUser.getRoles().add(employeeRole);
            }
            employeeUser.setFullName(request.employeeName());
            employeeUser.setActive(true); // Reactivate corporate account on reassignment
            employeeUser = userRepository.save(employeeUser);
        } else {
            employeeUser = new User();
            employeeUser.setFullName(request.employeeName());
            employeeUser.setUsername(request.employeeDasId());

            String[] nameParts = request.employeeName().trim().split("\\s+");
            String firstName = nameParts[0].toLowerCase();
            String surnameInitial = nameParts.length > 1
                    ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase()
                    : "";
            String defaultPassword = firstName + surnameInitial + "@123";

            employeeUser.setPassword(passwordEncoder.encode(defaultPassword));
            employeeUser.getRoles().add(employeeRole);
            employeeUser = userRepository.save(employeeUser);
        }

        EmployeeMembership membership = new EmployeeMembership();
        membership.setEmployee(employeeUser);
        membership.setTeam(team);
        membership.setManager(manager);
        membership.setGroup(group);
        membership.setActive(true);
        employeeMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public EditEmployeeRequest getEmployeeForEdit(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);
        EmployeeMembership membership = employeeMembershipRepository
                .findByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee does not have an active membership"));

        return new EditEmployeeRequest(
                employee.getFullName(),
                employee.getUsername(),
                membership.getGroup() != null ? membership.getGroup().getId() : null);
    }

    @Transactional
    public void editEmployee(Long managerId, Long employeeId, EditEmployeeRequest request) {
        User employee = validateAndGetEmployee(managerId, employeeId);

        EmployeeMembership membership = employeeMembershipRepository
                .findByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee does not have an active membership"));

        Group group = null;
        if (request.groupId() != null) {
            group = groupRepository
                    .findById(request.groupId())
                    .filter(g -> g.isActive()
                            && g.getTeam().getId().equals(membership.getTeam().getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid group selected"));
        }

        employee.setFullName(request.employeeName());

        if (!employee.getUsername().equals(request.username())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Employee DAS ID already exists");
            }
            employee.setUsername(request.username());
        }

        membership.setGroup(group);
        employeeMembershipRepository.save(membership);
    }

    @Transactional
    public void deactivateEmployee(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);
        employee.setActive(false);

        employeeMembershipRepository.findByEmployeeIdAndActiveTrue(employeeId).ifPresent(m -> m.setActive(false));
    }

    @Transactional
    public void deleteDeactivatedEmployee(Long managerId, Long employeeId) {
        List<EmployeeMembership> memberships = employeeMembershipRepository.findByEmployeeId(employeeId);
        EmployeeMembership targetMembership = memberships.stream()
                .filter(m -> m.getManager().getId().equals(managerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Employee membership not found or unauthorized"));

        if (targetMembership.isActive()) {
            throw new IllegalArgumentException("Cannot delete an active employee. Deactivate them first.");
        }

        employeeMembershipRepository.delete(targetMembership);
    }

    @Transactional
    public void resetEmployeePassword(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);

        String[] nameParts = employee.getFullName().trim().split("\\s+");
        String firstName = nameParts[0].toLowerCase();
        String surnameInitial = nameParts.length > 1
                ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase()
                : "";
        String defaultPassword = firstName + surnameInitial + "@123";

        employee.setPassword(passwordEncoder.encode(defaultPassword));
        employee.setPasswordChanged(false);
    }

    @Transactional
    public void configureMonthly(Long managerId, Long employeeId, MonthlyConfigRequest request) {
        User employee = validateAndGetEmployee(managerId, employeeId);

        MonthlyConfiguration config = monthlyConfigRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, request.month(), request.year())
                .orElse(new MonthlyConfiguration());

        config.setEmployee(employee);
        config.setMonth(request.month());
        config.setYear(request.year());

        int workingDays = calculateWorkingDays(request.year(), request.month());
        config.setWorkingDays(workingDays);

        config.setLeaves(request.leaves());
        config.setPublicHolidays(request.publicHolidays());
        config.setExceptionDays(request.exceptionDays());
        config.setManualCheckins(request.manualCheckins());

        int requiredDays =
                calculateRequiredDays(workingDays, request.leaves(), request.publicHolidays(), request.exceptionDays());
        config.setRequiredOfficeDays(requiredDays);

        monthlyConfigRepository.save(config);
    }

    @Transactional(readOnly = true)
    public MonthlyConfiguration getMonthlyConfig(Long managerId, Long employeeId, int month, int year) {
        validateAndGetEmployee(managerId, employeeId);
        return monthlyConfigRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseGet(() -> {
                    MonthlyConfiguration config = new MonthlyConfiguration();
                    config.setMonth(month);
                    config.setYear(year);
                    config.setWorkingDays(calculateWorkingDays(year, month));
                    config.setRequiredOfficeDays(calculateRequiredDays(config.getWorkingDays(), 0, 0, 0));
                    return config;
                });
    }

    @Transactional
    public List<Attendance> getAttendanceHistory(Long employeeId, int month, int year) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        List<Attendance> stale = attendanceRepository.findByEmployeeId(employeeId).stream()
                .filter(a -> a.getCheckOut() == null
                        && a.getOfficeDate().isBefore(today)
                        && !"FORGOT".equals(a.getAttendanceType()))
                .toList();

        if (!stale.isEmpty()) {
            for (Attendance a : stale) {
                a.setAttendanceType("FORGOT");
            }
            attendanceRepository.saveAll(stale);
        }
        return attendanceRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year);
    }

    private User validateAndGetEmployee(Long managerId, Long employeeId) {
        User employee = userRepository
                .findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        EmployeeMembership membership = employeeMembershipRepository
                .findByEmployeeIdAndActiveTrue(employeeId)
                .or(() -> employeeMembershipRepository.findByEmployeeId(employeeId).stream()
                        .filter(m -> m.getManager().getId().equals(managerId))
                        .findFirst())
                .orElseThrow(() -> new IllegalArgumentException("You are not authorized to manage this employee"));

        if (!membership.getManager().getId().equals(managerId)) {
            throw new IllegalArgumentException("You are not authorized to manage this employee");
        }

        return employee;
    }

    private int calculateWorkingDays(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        int workingDays = 0;

        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate date = LocalDate.of(year, month, i);
            if (date.getDayOfWeek().getValue() < 6) {
                workingDays++;
            }
        }
        return workingDays;
    }

    private int calculateRequiredDays(int workingDays, int leaves, int publicHoliday, int exceptionDays) {
        int effectiveDays = workingDays - leaves - publicHoliday - exceptionDays;
        return (int) Math.ceil(effectiveDays * 0.50);
    }
}
