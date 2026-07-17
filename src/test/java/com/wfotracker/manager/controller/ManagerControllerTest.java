package com.wfotracker.manager.controller;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ManagerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManagerService managerService;

    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private ManagerController managerController;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        User managerUser = new User();
        managerUser.setId(1L);
        managerUser.setUsername("manager");
        CustomUserDetails userDetails = new CustomUserDetails(managerUser);

        HandlerMethodArgumentResolver argumentResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(
                    org.springframework.core.MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(managerController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(argumentResolver)
                .setValidator(validator)
                .build();
    }

    @Test
    void testDashboard_Default() throws Exception {
        when(managerService.getMembershipsForManager(eq(1L), any())).thenReturn(Collections.emptyList());
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-dashboard"))
                .andExpect(model().attributeExists(
                                "employeesCompliance", "currentMonth", "currentYear", "groups", "showDefault"));
    }

    @Test
    void testShowAddEmployeeForm() throws Exception {
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attributeExists("addEmployeeRequest", "groups"));
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
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/manager/employee/add").param("employeeName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"));
    }

    @Test
    void testListEmployees() throws Exception {
        when(managerService.getMembershipsForManager(1L, null)).thenReturn(Collections.emptyList());
        when(managerService.getEmployeesForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-list"))
                .andExpect(model().attributeExists("memberships", "employees"));
    }

    @Test
    void testShowEditEmployeeForm() throws Exception {
        EditEmployeeRequest editRequest = new EditEmployeeRequest("Jane", "jane", null);
        when(managerService.getEmployeeForEdit(1L, 2L)).thenReturn(editRequest);
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/edit/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit"))
                .andExpect(model().attribute("employeeId", 2L))
                .andExpect(model().attribute("editEmployeeRequest", editRequest))
                .andExpect(model().attributeExists("groups"));
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
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/manager/employee/edit/2")
                        .param("employeeName", "")
                        .param("username", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit"))
                .andExpect(model().attribute("employeeId", 2L))
                .andExpect(model().attributeExists("groups"));
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
    void testDeleteEmployee_Success() throws Exception {
        mockMvc.perform(post("/manager/employee/delete/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/employee/list"))
                .andExpect(flash().attribute("success", "Employee membership permanently deleted."));

        verify(managerService).deleteDeactivatedEmployee(1L, 2L);
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

    @Test
    void testListGroups() throws Exception {
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/group/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("group-list"))
                .andExpect(model().attributeExists("groups"));
    }

    @Test
    void testShowCreateGroupForm() throws Exception {
        mockMvc.perform(get("/manager/group/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("group-form"))
                .andExpect(model().attribute("groupName", ""));
    }

    @Test
    void testCreateGroup_Success() throws Exception {
        mockMvc.perform(post("/manager/group/create").param("groupName", "Engineering"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/group/list"))
                .andExpect(flash().attribute("success", "Group created successfully."));

        verify(managerService).createGroup(1L, "Engineering");
    }

    @Test
    void testDeleteGroup_Success() throws Exception {
        mockMvc.perform(post("/manager/group/delete/30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/group/list"))
                .andExpect(flash().attribute(
                                "success",
                                "Group permanently deleted. All assigned employees moved to DEFAULT group."));

        verify(managerService).deleteGroup(1L, 30L);
    }

    @Test
    void testViewEmployeeHistory() throws Exception {
        when(managerService.getAttendanceHistory(2L, 7, 2026)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/history/2").param("month", "7").param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance-history"))
                .andExpect(model().attributeExists("history", "currentMonth", "currentYear", "employeeId", "today"));
    }

    @Test
    void testAddEmployee_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Employee DAS ID already exists"))
                .when(managerService)
                .addEmployee(eq(1L), any());
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/manager/employee/add")
                        .param("employeeName", "Jane Employee")
                        .param("employeeDasId", "jane"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-form"))
                .andExpect(model().attribute("error", "Employee DAS ID already exists"));
    }

    @Test
    void testEditEmployee_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Username already exists"))
                .when(managerService)
                .editEmployee(eq(1L), eq(2L), any());
        when(managerService.getGroupsForManager(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/manager/employee/edit/2")
                        .param("employeeName", "Jane New")
                        .param("username", "janenew"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-edit"))
                .andExpect(model().attribute("employeeId", 2L))
                .andExpect(model().attribute("error", "Username already exists"));
    }

    @Test
    void testDeleteEmployee_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Permanently delete failed"))
                .when(managerService)
                .deleteDeactivatedEmployee(1L, 2L);

        mockMvc.perform(post("/manager/employee/delete/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/employee/list"))
                .andExpect(flash().attribute("error", "Permanently delete failed"));
    }

    @Test
    void testResetEmployeePassword_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Reset failed"))
                .when(managerService)
                .resetEmployeePassword(1L, 2L);

        mockMvc.perform(post("/manager/employee/reset-password/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"))
                .andExpect(flash().attribute("error", "Reset failed"));
    }

    @Test
    void testConfigureEmployee_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Config failed"))
                .when(managerService)
                .configureMonthly(eq(1L), eq(2L), any());

        mockMvc.perform(post("/manager/employee/configure/2")
                        .param("leaves", "1")
                        .param("publicHolidays", "1")
                        .param("exceptionDays", "1")
                        .param("manualCheckins", "1")
                        .param("month", "7")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee-config"))
                .andExpect(model().attribute("employeeId", 2L))
                .andExpect(model().attribute("error", "Config failed"));
    }

    @Test
    void testCreateGroup_ValidationError() throws Exception {
        mockMvc.perform(post("/manager/group/create").param("groupName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("group-form"))
                .andExpect(model().attribute("error", "Group name cannot be empty"));
    }

    @Test
    void testCreateGroup_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Group name exists"))
                .when(managerService)
                .createGroup(1L, "Engineering");

        mockMvc.perform(post("/manager/group/create").param("groupName", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(view().name("group-form"))
                .andExpect(model().attribute("error", "Group name exists"));
    }

    @Test
    void testDeleteGroup_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Delete group failed"))
                .when(managerService)
                .deleteGroup(1L, 30L);

        mockMvc.perform(post("/manager/group/delete/30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/group/list"))
                .andExpect(flash().attribute("error", "Delete group failed"));
    }

    @Test
    void testExportToExcel_Success() throws Exception {
        when(managerService.getMembershipsForManager(eq(1L), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/export/xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void testExportToCsv_Success() throws Exception {
        when(managerService.getMembershipsForManager(eq(1L), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }

    @Test
    void testExportEmployeesExcel_Success() throws Exception {
        when(managerService.getMembershipsForManager(eq(1L), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/export/xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void testExportEmployeesCsv_Success() throws Exception {
        when(managerService.getMembershipsForManager(eq(1L), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/manager/employee/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }
}
