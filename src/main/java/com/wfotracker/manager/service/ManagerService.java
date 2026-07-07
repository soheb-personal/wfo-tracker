package com.wfotracker.manager.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.EmployeeMembership;
import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.EmployeeMembershipRepository;
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

    private final UserRepository userRepository;
    private final MonthlyConfigurationRepository monthlyConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final EmployeeMembershipRepository employeeMembershipRepository;
    private final org.springframework.security.core.session.SessionRegistry sessionRegistry;

    @Transactional(readOnly = true)
    public List<User> getEmployeesForManager(Long managerId) {
        return userRepository.findByManagerIdAndRoleAndActiveTrue(managerId, Role.EMPLOYEE);
    }

    @Transactional
    public void addEmployee(Long managerId, AddEmployeeRequest request) {
        User manager =
                userRepository.findById(managerId).orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        TeamManager teamManager = teamManagerRepository
                .findByManagerIdAndActiveTrue(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager is not currently assigned to any team"));
        Team team = teamManager.getTeam();

        com.wfotracker.domain.entity.Role employeeRole = roleRepository
                .findByName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException("ROLE_EMPLOYEE role not found"));

        User employeeUser;
        Optional<User> existingUserOpt = userRepository.findByUsername(request.employeeDasId());

        if (existingUserOpt.isPresent()) {
            employeeUser = existingUserOpt.get();
            // Validate that the employee is not already active in another team
            if (employeeMembershipRepository
                    .findByEmployeeIdAndActiveTrue(employeeUser.getId())
                    .isPresent()) {
                throw new IllegalArgumentException("Employee is already active in another team");
            }
            // Add ROLE_EMPLOYEE role if they don't have it
            if (!employeeUser.getRoles().contains(employeeRole)) {
                employeeUser.getRoles().add(employeeRole);
            }
            // Update full name to the latest input
            employeeUser.setFullName(request.employeeName());
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
        membership.setActive(true);
        employeeMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public EditEmployeeRequest getEmployeeForEdit(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);
        return new EditEmployeeRequest(employee.getFullName(), employee.getUsername());
    }

    @Transactional
    public void editEmployee(Long managerId, Long employeeId, EditEmployeeRequest request) {
        User employee = validateAndGetEmployee(managerId, employeeId);

        employee.setFullName(request.employeeName());

        if (!employee.getUsername().equals(request.username())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Employee DAS ID already exists");
            }
            employee.setUsername(request.username());
        }
    }

    @Transactional
    public void deactivateEmployee(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);
        employee.setActive(false);

        employeeMembershipRepository.findByEmployeeIdAndActiveTrue(employeeId).ifPresent(m -> m.setActive(false));
        expireUserSessions(employeeId);
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

    private User validateAndGetEmployee(Long managerId, Long employeeId) {
        User employee = userRepository
                .findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        EmployeeMembership membership = employeeMembershipRepository
                .findByEmployeeIdAndActiveTrue(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee does not have an active membership"));

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
            if (date.getDayOfWeek().getValue() < 6) { // Monday to Friday
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
