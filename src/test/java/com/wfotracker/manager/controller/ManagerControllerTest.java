package com.wfotracker.manager.controller;

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
import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.User;
import com.wfotracker.manager.dto.EditEmployeeRequest;
import com.wfotracker.manager.service.ManagerService;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManagerService managerService;

    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private ManagerController managerController;

    private User manager;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setFullName("John Manager");
        manager.setUsername("john");

        userDetails = new CustomUserDetails(manager);

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

        mockMvc = MockMvcBuilders.standaloneSetup(managerController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(argumentResolver)
                .build();
    }

    @Test
    void testDashboard_Default() throws Exception {
        when(managerService.getEmployeesForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-dashboard"))
                .andExpect(model().attributeExists("employeesCompliance", "currentMonth", "currentYear"));
    }

    @Test
    void testShowAddEmployeeForm() throws Exception {
        mockMvc.perform(get("/manager/employee/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attributeExists("addEmployeeRequest"));
    }

    @Test
    void testAddEmployee_Success() throws Exception {
        mockMvc.perform(post("/manager/employee/add")
                        .param("employeeName", "Jane Employee")
                        .param("employeeDasId", "jane"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("success", "Employee added successfully."));
    }

    @Test
    void testAddEmployee_ValidationError() throws Exception {
        mockMvc.perform(post("/manager/employee/add").param("employeeName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"));
    }

    @Test
    void testListEmployees() throws Exception {
        when(managerService.getEmployeesForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-list"))
                .andExpect(model().attributeExists("employees"));
    }

    @Test
    void testShowEditEmployeeForm() throws Exception {
        EditEmployeeRequest editRequest = new EditEmployeeRequest("Jane", "jane");
        when(managerService.getEmployeeForEdit(1L, 2L)).thenReturn(editRequest);

        mockMvc.perform(get("/manager/employee/edit/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit"))
                .andExpect(model().attribute("employeeId", 2L))
                .andExpect(model().attribute("editEmployeeRequest", editRequest));
    }

    @Test
    void testEditEmployee_Success() throws Exception {
        mockMvc.perform(post("/manager/employee/edit/2")
                        .param("employeeName", "Jane New")
                        .param("username", "janenew"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("success", "Employee updated successfully."));
    }

    @Test
    void testEditEmployee_ValidationError() throws Exception {
        mockMvc.perform(post("/manager/employee/edit/2")
                        .param("employeeName", "")
                        .param("username", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit"))
                .andExpect(model().attribute("employeeId", 2L));
    }

    @Test
    void testDeactivateEmployee_Success() throws Exception {
        mockMvc.perform(post("/manager/employee/deactivate/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("success", "Employee deactivated successfully."));

        verify(managerService).deactivateEmployee(1L, 2L);
    }

    @Test
    void testDeactivateEmployee_Error() throws Exception {
        doThrow(new IllegalArgumentException("Error")).when(managerService).deactivateEmployee(1L, 2L);

        mockMvc.perform(post("/manager/employee/deactivate/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("error", "Error"));
    }

    @Test
    void testResetEmployeePassword_Success() throws Exception {
        mockMvc.perform(post("/manager/employee/reset-password/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("success", "Employee password reset successfully."));
    }

    @Test
    void testShowConfigureEmployeeForm() throws Exception {
        MonthlyConfiguration config = new MonthlyConfiguration();
        config.setLeaves(2);
        config.setPublicHolidays(1);
        config.setExceptionDays(0);
        config.setManualCheckins(1);

        when(managerService.getMonthlyConfig(eq(1L), eq(2L), anyInt(), anyInt()))
                .thenReturn(config);

        mockMvc.perform(get("/manager/employee/configure/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-config"))
                .andExpect(model().attributeExists("monthlyConfigRequest", "employeeId"));
    }

    @Test
    void testConfigureEmployee_Success() throws Exception {
        mockMvc.perform(post("/manager/employee/configure/2")
                        .param("leaves", "2")
                        .param("publicHolidays", "1")
                        .param("exceptionDays", "0")
                        .param("manualCheckins", "1")
                        .param("month", "7")
                        .param("year", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("success", "Monthly configuration updated."));
    }
}
