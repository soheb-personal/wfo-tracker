package com.wfotracker.compliance.service;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.manager.dto.EmployeeComplianceDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplianceServiceTest {

    @Test
    void testCalculateRequiredDays() throws Exception {
        ComplianceService complianceService = new ComplianceService(null, null);

        Method calculateRequiredDays = ComplianceService.class.getDeclaredMethod(
                "calculateRequiredDays", int.class, int.class, int.class, int.class);
        calculateRequiredDays.setAccessible(true);

        int result = (int) calculateRequiredDays.invoke(complianceService, 23, 1, 1, 0);
        assertEquals(11, result, "Should calculate 50% rounded up correctly");

        int result2 = (int) calculateRequiredDays.invoke(complianceService, 22, 0, 0, 0);
        assertEquals(11, result2, "Should calculate 50% for even number correctly");

        int result3 = (int) calculateRequiredDays.invoke(complianceService, 21, 0, 0, 0);
        assertEquals(11, result3, "Should calculate 50% rounded up correctly");
    }

    @Test
    void testComplianceWithManualCheckins() {
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        MonthlyConfigurationRepository monthlyConfigRepository = mock(MonthlyConfigurationRepository.class);
        ComplianceService complianceService = new ComplianceService(attendanceRepository, monthlyConfigRepository);

        User employee = new User();
        employee.setId(1L);
        employee.setFullName("John Doe");
        employee.setUsername("johndoe");
        employee.setActive(true);

        MonthlyConfiguration config = new MonthlyConfiguration();
        config.setEmployee(employee);
        config.setMonth(7);
        config.setYear(2026);
        config.setWorkingDays(23);
        config.setLeaves(2);
        config.setPublicHolidays(1);
        config.setExceptionDays(2);
        config.setRequiredOfficeDays(9); // (23 - 2 - 1 - 2) * 50% = 18 * 0.5 = 9
        config.setManualCheckins(3); // 3 manual checkins

        when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(1L, 7, 2026))
                .thenReturn(Optional.of(config));

        // DB attendance records = 4 days
        when(attendanceRepository.countVisitedDaysByEmployeeIdAndMonthAndYear(1L, 7, 2026))
                .thenReturn(4);

        EmployeeComplianceDto dto = complianceService.getComplianceForEmployee(employee, 7, 2026);

        // Expected visited days = 4 (DB) + 3 (Manual) = 7
        assertEquals(7, dto.actualOfficeDaysVisited(), "Visited days should include manual checkins");
        assertEquals(9, dto.requiredOfficeDays(), "Required office days should be 9");
        assertEquals(2, dto.remainingOfficeDays(), "Remaining office days should be 2 (9 - 7)");
        // Compliance % = round((7/9)*100) = round(77.777) = 78%
        assertEquals(78, dto.compliancePercentage(), "Compliance percentage should be 78%");
    }

    @Test
    void testComplianceDefaultFallback() {
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        MonthlyConfigurationRepository monthlyConfigRepository = mock(MonthlyConfigurationRepository.class);
        ComplianceService complianceService = new ComplianceService(attendanceRepository, monthlyConfigRepository);

        User employee = new User();
        employee.setId(2L);
        employee.setFullName("Jane Smith");
        employee.setUsername("janesmith");
        employee.setActive(true);

        when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(2L, 7, 2026))
                .thenReturn(Optional.empty());
        when(attendanceRepository.countVisitedDaysByEmployeeIdAndMonthAndYear(2L, 7, 2026))
                .thenReturn(3);

        EmployeeComplianceDto dto = complianceService.getComplianceForEmployee(employee, 7, 2026);

        // Required days fallback working days: July 2026 has 23 working days. Required days = ceil(23*0.50) = 12.
        assertEquals(12, dto.requiredOfficeDays(), "Fallback required office days should be 12");
        assertEquals(3, dto.actualOfficeDaysVisited(), "Actual visited days should be 3");
        assertEquals(9, dto.remainingOfficeDays(), "Remaining office days should be 9");
        // Compliance % = round((3/12)*100) = 25%
        assertEquals(25, dto.compliancePercentage(), "Compliance percentage should be 25%");
    }

    @Test
    void testComplianceForEmployeeWithActiveStatus() {
        AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
        MonthlyConfigurationRepository monthlyConfigRepository = mock(MonthlyConfigurationRepository.class);
        ComplianceService complianceService = new ComplianceService(attendanceRepository, monthlyConfigRepository);

        User employee = new User();
        employee.setId(3L);
        employee.setFullName("Mark Green");
        employee.setUsername("markgreen");
        employee.setActive(true);

        when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(3L, 7, 2026))
                .thenReturn(Optional.empty());
        when(attendanceRepository.countVisitedDaysByEmployeeIdAndMonthAndYear(3L, 7, 2026))
                .thenReturn(1);

        // Call the 4-parameter method directly
        EmployeeComplianceDto dto = complianceService.getComplianceForEmployee(employee, false, 7, 2026);

        // Verify active status is set to false in the returned DTO
        org.junit.jupiter.api.Assertions.assertFalse(dto.active(), "Active status should match the passed parameter");
    }
}
