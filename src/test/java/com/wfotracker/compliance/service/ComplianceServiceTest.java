package com.wfotracker.compliance.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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
}
