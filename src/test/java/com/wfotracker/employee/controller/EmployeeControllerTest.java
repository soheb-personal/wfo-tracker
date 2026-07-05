package com.wfotracker.employee.controller;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.compliance.service.ComplianceService;
import com.wfotracker.domain.entity.User;
import com.wfotracker.employee.service.EmployeeService;
import com.wfotracker.manager.dto.EmployeeComplianceDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private EmployeeController employeeController;

    private User employee;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setFullName("Jane Employee");
        employee.setUsername("jane");

        userDetails = new CustomUserDetails(employee);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        HandlerMethodArgumentResolver argumentResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(employeeController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(argumentResolver)
                .build();
    }

    @Test
    void testDashboard() throws Exception {
        EmployeeComplianceDto compliance =
                new EmployeeComplianceDto(1L, "Jane", "jane", true, 20, 0, 0, 0, 0, 10, 5, 5, 50);
        when(complianceService.getComplianceForEmployee(any(User.class), anyInt(), anyInt()))
                .thenReturn(compliance);
        when(employeeService.isCheckedInToday(1L)).thenReturn(false);
        when(employeeService.isCheckedOutToday(1L)).thenReturn(false);

        mockMvc.perform(get("/employee/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-dashboard"))
                .andExpect(model().attribute("compliance", compliance));
    }

    @Test
    void testCheckIn_Success() throws Exception {
        mockMvc.perform(post("/employee/checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andExpect(flash().attribute("success", "Checked in successfully."));
    }

    @Test
    void testCheckIn_Failure() throws Exception {
        doThrow(new IllegalStateException("Already checked in"))
                .when(employeeService)
                .checkIn(1L);

        mockMvc.perform(post("/employee/checkin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andExpect(flash().attribute("error", "Already checked in"));
    }

    @Test
    void testCheckOut_Success() throws Exception {
        mockMvc.perform(post("/employee/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andExpect(flash().attribute("success", "Checked out successfully."));
    }

    @Test
    void testCheckOut_Failure() throws Exception {
        doThrow(new IllegalStateException("No check-in found"))
                .when(employeeService)
                .checkOut(1L);

        mockMvc.perform(post("/employee/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"))
                .andExpect(flash().attribute("error", "No check-in found"));
    }

    @Test
    void testHistory() throws Exception {
        when(employeeService.getAttendanceHistory(eq(1L), anyInt(), anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/employee/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance-history"))
                .andExpect(model().attributeExists("history", "currentMonth", "currentYear"));
    }
}
