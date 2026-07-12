package com.wfotracker.admin.controller;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.wfotracker.admin.dto.CreateTeamRequest;
import com.wfotracker.admin.dto.EditTeamRequest;
import com.wfotracker.admin.dto.TeamDto;
import com.wfotracker.admin.service.AdminService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void testDashboard() throws Exception {
        TeamDto dto = new TeamDto(1L, "Team A", 2L, "Mgr", "mgr", true, Collections.emptyList());
        when(adminService.getAllTeams()).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attributeExists("teams"))
                .andExpect(model().attribute("teams", List.of(dto)));
    }

    @Test
    void testShowCreateTeamForm() throws Exception {
        mockMvc.perform(get("/admin/team/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("team-form"))
                .andExpect(model().attributeExists("createTeamRequest"));
    }

    @Test
    void testCreateTeam_ValidationError() throws Exception {
        mockMvc.perform(post("/admin/team/create").param("teamName", "").param("managerName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("team-form"));

        verify(adminService, never()).createTeam(any());
    }

    @Test
    void testCreateTeam_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Team name already exists"))
                .when(adminService)
                .createTeam(any(CreateTeamRequest.class));

        mockMvc.perform(post("/admin/team/create")
                        .param("teamName", "Team A")
                        .param("managerName", "Rahul Sharma")
                        .param("managerDasId", "rahul"))
                .andExpect(status().isOk())
                .andExpect(view().name("team-form"))
                .andExpect(model().attribute("error", "Team name already exists"));
    }

    @Test
    void testCreateTeam_Success() throws Exception {
        mockMvc.perform(post("/admin/team/create")
                        .param("teamName", "Team A")
                        .param("managerName", "Rahul Sharma")
                        .param("managerDasId", "rahul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Team and Manager created successfully."));

        verify(adminService).createTeam(any(CreateTeamRequest.class));
    }

    @Test
    void testListTeams() throws Exception {
        TeamDto dto = new TeamDto(1L, "Team A", 2L, "Mgr", "mgr", true, Collections.emptyList());
        when(adminService.getAllTeams()).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/team/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("team-list"))
                .andExpect(model().attributeExists("teams"));
    }

    @Test
    void testShowEditTeamForm() throws Exception {
        EditTeamRequest request = new EditTeamRequest("Team A", "Rahul Sharma", "rahul");
        when(adminService.getTeamForEdit(1L)).thenReturn(request);

        mockMvc.perform(get("/admin/team/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("team-edit"))
                .andExpect(model().attribute("editTeamRequest", request))
                .andExpect(model().attribute("teamId", 1L));
    }

    @Test
    void testEditTeam_ValidationError() throws Exception {
        mockMvc.perform(post("/admin/team/edit/1")
                        .param("teamName", "")
                        .param("managerName", "")
                        .param("managerUsername", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("team-edit"))
                .andExpect(model().attribute("teamId", 1L));

        verify(adminService, never()).editTeam(any(), any());
    }

    @Test
    void testEditTeam_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Team name already exists"))
                .when(adminService)
                .editTeam(eq(1L), any(EditTeamRequest.class));

        mockMvc.perform(post("/admin/team/edit/1")
                        .param("teamName", "Team A")
                        .param("managerName", "Rahul Sharma")
                        .param("managerUsername", "rahul"))
                .andExpect(status().isOk())
                .andExpect(view().name("team-edit"))
                .andExpect(model().attribute("error", "Team name already exists"))
                .andExpect(model().attribute("teamId", 1L));
    }

    @Test
    void testEditTeam_Success() throws Exception {
        mockMvc.perform(post("/admin/team/edit/1")
                        .param("teamName", "Team A")
                        .param("managerName", "Rahul Sharma")
                        .param("managerUsername", "rahul"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Team updated successfully."));

        verify(adminService).editTeam(eq(1L), any(EditTeamRequest.class));
    }

    @Test
    void testDeactivateTeam_Success() throws Exception {
        mockMvc.perform(post("/admin/team/deactivate/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Team deactivated successfully."));

        verify(adminService).deactivateTeam(1L);
    }

    @Test
    void testDeactivateTeam_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Team not found"))
                .when(adminService)
                .deactivateTeam(1L);

        mockMvc.perform(post("/admin/team/deactivate/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("error", "Team not found"));
    }

    @Test
    void testResetManagerPassword_Success() throws Exception {
        mockMvc.perform(post("/admin/manager/reset-password/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("success", "Manager password reset successfully."));

        verify(adminService).resetManagerPassword(2L);
    }

    @Test
    void testResetManagerPassword_ServiceException() throws Exception {
        doThrow(new IllegalArgumentException("Manager not found"))
                .when(adminService)
                .resetManagerPassword(2L);

        mockMvc.perform(post("/admin/manager/reset-password/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attribute("error", "Manager not found"));
    }

    @Test
    void testExportToExcel_Success() throws Exception {
        TeamDto teamDto = new TeamDto(1L, "Team A", 2L, "Mgr", "mgr", true, Collections.emptyList());
        when(adminService.getAllTeams()).thenReturn(List.of(teamDto));

        mockMvc.perform(get("/admin/export/xlsx"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void testExportToCsv_Success() throws Exception {
        TeamDto teamDto = new TeamDto(1L, "Team A", 2L, "Mgr", "mgr", true, Collections.emptyList());
        when(adminService.getAllTeams()).thenReturn(List.of(teamDto));

        mockMvc.perform(get("/admin/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }
}
