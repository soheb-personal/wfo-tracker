package com.wfotracker.manager.service;

import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.domain.repository.UserRepository;
import com.wfotracker.manager.dto.AddEmployeeRequest;
import com.wfotracker.manager.dto.EditEmployeeRequest;
import com.wfotracker.manager.dto.MonthlyConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final UserRepository userRepository;
    private final MonthlyConfigurationRepository monthlyConfigRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> getEmployeesForManager(Long managerId) {
        return userRepository.findByManagerIdAndRoleAndActiveTrue(managerId, Role.EMPLOYEE);
    }

    @Transactional
    public void addEmployee(Long managerId, AddEmployeeRequest request) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));

        User employee = new User();
        employee.setFullName(request.employeeName());

        String[] nameParts = request.employeeName().trim().split("\\s+");
        String firstName = nameParts[0].toLowerCase();
        String username = getUniqueUsername(firstName);
        employee.setUsername(username);

        String surnameInitial = nameParts.length > 1 ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase() : "";
        String defaultPassword = firstName + surnameInitial + "@123";

        employee.setPassword(passwordEncoder.encode(defaultPassword));
        employee.setRole(Role.EMPLOYEE);
        employee.setManager(manager);
        employee.setTeam(manager.getTeam());

        userRepository.save(employee);
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
                throw new IllegalArgumentException("Username already exists");
            }
            employee.setUsername(request.username());
        }
    }

    @Transactional
    public void deactivateEmployee(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);
        employee.setActive(false);
    }

    @Transactional
    public void resetEmployeePassword(Long managerId, Long employeeId) {
        User employee = validateAndGetEmployee(managerId, employeeId);

        String[] nameParts = employee.getFullName().trim().split("\\s+");
        String firstName = nameParts[0].toLowerCase();
        String surnameInitial = nameParts.length > 1 ? nameParts[nameParts.length - 1].substring(0, 1).toLowerCase() : "";
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
        
        int requiredDays = calculateRequiredDays(workingDays, request.leaves(), request.publicHolidays(), request.exceptionDays());
        config.setRequiredOfficeDays(requiredDays);

        monthlyConfigRepository.save(config);
    }
    
    @Transactional(readOnly = true)
    public MonthlyConfiguration getMonthlyConfig(Long managerId, Long employeeId, int month, int year) {
        validateAndGetEmployee(managerId, employeeId);
        return monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
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
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getManager().getId().equals(managerId)) {
            throw new IllegalArgumentException("You are not authorized to manage this employee");
        }

        return employee;
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
