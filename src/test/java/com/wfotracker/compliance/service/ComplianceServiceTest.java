package com.wfotracker.compliance.service;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.manager.dto.EmployeeComplianceDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        AttendanceRepository attendanceRepository = Mockito.mock(AttendanceRepository.class);
        MonthlyConfigurationRepository monthlyConfigRepository = Mockito.mock(MonthlyConfigurationRepository.class);
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

        Mockito.when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(1L, 7, 2026))
                .thenReturn(Optional.of(config));

        // DB attendance records = 4 days
        Mockito.when(attendanceRepository.countVisitedDaysByEmployeeIdAndMonthAndYear(1L, 7, 2026))
                .thenReturn(4);

        EmployeeComplianceDto dto = complianceService.getComplianceForEmployee(employee, 7, 2026);

        // Expected visited days = 4 (DB) + 3 (Manual) = 7
        assertEquals(7, dto.actualOfficeDaysVisited(), "Visited days should include manual checkins");
        assertEquals(9, dto.requiredOfficeDays(), "Required office days should be 9");
        assertEquals(2, dto.remainingOfficeDays(), "Remaining office days should be 2 (9 - 7)");
        // Compliance % = round((7/9)*100) = round(77.777) = 78%
        assertEquals(78, dto.compliancePercentage(), "Compliance percentage should be 78%");
    }
}
