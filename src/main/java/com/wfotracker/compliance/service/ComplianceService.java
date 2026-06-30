package com.wfotracker.compliance.service;

import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.manager.dto.EmployeeComplianceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final AttendanceRepository attendanceRepository;
    private final MonthlyConfigurationRepository monthlyConfigRepository;

    @Transactional(readOnly = true)
    public EmployeeComplianceDto getComplianceForEmployee(User employee, int month, int year) {
        
        MonthlyConfiguration config = monthlyConfigRepository
                .findByEmployeeIdAndMonthAndYear(employee.getId(), month, year)
                .orElseGet(() -> {
                    MonthlyConfiguration defaultConfig = new MonthlyConfiguration();
                    defaultConfig.setWorkingDays(calculateWorkingDays(year, month));
                    defaultConfig.setRequiredOfficeDays(calculateRequiredDays(defaultConfig.getWorkingDays(), 0, 0, 0));
                    return defaultConfig;
                });

        int visitedDays = attendanceRepository.countVisitedDaysByEmployeeIdAndMonthAndYear(employee.getId(), month, year);
        int requiredDays = config.getRequiredOfficeDays();
        int remainingDays = Math.max(0, requiredDays - visitedDays);
        int compliancePercentage = requiredDays > 0 ? (int) Math.round(((double) visitedDays / requiredDays) * 100) : 100;
        if (compliancePercentage > 100) compliancePercentage = 100;

        return new EmployeeComplianceDto(
                employee.getId(),
                employee.getFullName(),
                employee.getUsername(),
                employee.isActive(),
                config.getWorkingDays(),
                config.getLeaves(),
                config.getPublicHolidays(),
                config.getExceptionDays(),
                requiredDays,
                visitedDays,
                remainingDays,
                compliancePercentage
        );
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
