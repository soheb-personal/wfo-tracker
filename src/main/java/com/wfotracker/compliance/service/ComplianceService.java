package com.wfotracker.compliance.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.manager.dto.EmployeeComplianceDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final AttendanceRepository attendanceRepository;
    private final MonthlyConfigurationRepository monthlyConfigRepository;

    @Transactional(readOnly = true)
    public EmployeeComplianceDto getComplianceForEmployee(User employee, int month, int year) {
        return calculateCompliance(employee, employee.isActive(), month, year);
    }

    @Transactional(readOnly = true)
    public EmployeeComplianceDto getComplianceForEmployee(User employee, boolean active, int month, int year) {
        return calculateCompliance(employee, active, month, year);
    }

    private EmployeeComplianceDto calculateCompliance(User employee, boolean active, int month, int year) {
        MonthlyConfiguration config = monthlyConfigRepository
                .findByEmployeeIdAndMonthAndYear(employee.getId(), month, year)
                .orElseGet(() -> {
                    MonthlyConfiguration defaultConfig = new MonthlyConfiguration();
                    defaultConfig.setWorkingDays(calculateWorkingDays(year, month));
                    defaultConfig.setRequiredOfficeDays(calculateRequiredDays(defaultConfig.getWorkingDays(), 0, 0, 0));
                    return defaultConfig;
                });

        int recordedVisitedDays =
                attendanceRepository.countVisitedDaysByEmployeeIdAndMonthAndYear(employee.getId(), month, year);
        int visitedDays = recordedVisitedDays + config.getManualCheckins();
        int requiredDays = config.getRequiredOfficeDays();
        int remainingDays = Math.max(0, requiredDays - visitedDays);
        int compliancePercentage =
                requiredDays > 0 ? (int) Math.round(((double) visitedDays / requiredDays) * 100) : 100;
        if (compliancePercentage > 100) {
            compliancePercentage = 100;
        }

        return new EmployeeComplianceDto(
                employee.getId(),
                employee.getFullName(),
                employee.getUsername(),
                active,
                config.getWorkingDays(),
                config.getLeaves(),
                config.getPublicHolidays(),
                config.getExceptionDays(),
                config.getManualCheckins(),
                requiredDays,
                visitedDays,
                remainingDays,
                compliancePercentage);
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
