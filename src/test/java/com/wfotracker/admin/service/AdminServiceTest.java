package com.wfotracker.admin.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wfotracker.admin.dto.CreateTeamRequest;
import com.wfotracker.admin.dto.EditTeamRequest;
import com.wfotracker.admin.dto.TeamDto;
import com.wfotracker.common.constants.Role;
import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.TeamRepository;
import com.wfotracker.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private org.springframework.security.core.session.SessionRegistry sessionRegistry;

    @InjectMocks
    private AdminService adminService;

    private Team team;
    private User manager;
    private com.wfotracker.domain.entity.Role managerRoleEntity;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(1L);
        team.setTeamName("Test Team");
        team.setActive(true);

        manager = new User();
        manager.setId(2L);
        manager.setFullName("Rahul Sharma");
        manager.setUsername("rahul");
        manager.setPassword("encodedPassword");
        manager.setActive(true);

        managerRoleEntity = new com.wfotracker.domain.entity.Role();
        managerRoleEntity.setId(1L);
        managerRoleEntity.setName("ROLE_MANAGER");
        manager.getRoles().add(managerRoleEntity);
    }

    @Test
    void testGetAllTeams() {
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(userRepository.findByTeamIdAndRole(1L, Role.MANAGER)).thenReturn(List.of(manager));

        User employee = new User();
        employee.setId(3L);
        employee.setFullName("John Doe");
        employee.setUsername("johndoe");
        employee.setActive(true);
        when(userRepository.findByTeamIdAndRole(1L, Role.EMPLOYEE)).thenReturn(List.of(employee));

        List<TeamDto> result = adminService.getAllTeams();

        assertEquals(1, result.size());
        TeamDto dto = result.getFirst();
        assertEquals(team.getId(), dto.id());
        assertEquals(team.getTeamName(), dto.teamName());
        assertEquals(manager.getId(), dto.managerId());
        assertEquals(manager.getFullName(), dto.managerName());
        assertEquals(manager.getUsername(), dto.managerUsername());
        assertTrue(dto.active());
        assertEquals(1, dto.employees().size());
        assertEquals(employee.getFullName(), dto.employees().getFirst().fullName());
    }

    @Test
    void testCreateTeam_NameExists() {
        CreateTeamRequest request = new CreateTeamRequest("Test Team", "Rahul Sharma", "rahul");
        when(teamRepository.existsByTeamName("Test Team")).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.createTeam(request));
        assertEquals("Team name already exists", exception.getMessage());
    }

    @Test
    void testCreateTeam_DasIdAlreadyExistsAndAlreadyActiveManager() {
        CreateTeamRequest request = new CreateTeamRequest("New Team", "Rahul Sharma", "rahul");
        when(teamRepository.existsByTeamName("New Team")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.of(managerRoleEntity));
        when(userRepository.findByUsername("rahul")).thenReturn(Optional.of(manager));
        when(teamManagerRepository.existsByManagerIdAndActiveTrue(manager.getId()))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.createTeam(request));
        assertEquals("This user is already an active manager of another team", exception.getMessage());
    }

    @Test
    void testCreateTeam_RoleNotFound() {
        CreateTeamRequest request = new CreateTeamRequest("New Team", "Rahul Sharma", "rahul");
        when(teamRepository.existsByTeamName("New Team")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.empty());

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> adminService.createTeam(request));
        assertEquals("ROLE_MANAGER role not found", exception.getMessage());
    }

    @Test
    void testCreateTeam_Success() {
        CreateTeamRequest request = new CreateTeamRequest("New Team", "Rahul Sharma", "rahul");
        when(teamRepository.existsByTeamName("New Team")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.of(managerRoleEntity));
        when(userRepository.findByUsername("rahul")).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });

        when(passwordEncoder.encode("rahuls@123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        adminService.createTeam(request);

        verify(teamRepository).save(any(Team.class));
        verify(userRepository).save(any(User.class));
        verify(teamManagerRepository).save(any(TeamManager.class));
    }

    @Test
    void testCreateTeam_Success_SingleName() {
        CreateTeamRequest request = new CreateTeamRequest("New Team", "Rahul", "rahul");
        when(teamRepository.existsByTeamName("New Team")).thenReturn(false);
        when(roleRepository.findByName("ROLE_MANAGER")).thenReturn(Optional.of(managerRoleEntity));
        when(userRepository.findByUsername("rahul")).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });
        when(passwordEncoder.encode("rahul@123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        adminService.createTeam(request);

        verify(teamRepository).save(any(Team.class));
        verify(userRepository).save(any(User.class));
        verify(teamManagerRepository).save(any(TeamManager.class));
    }

    @Test
    void testGetTeamForEdit_TeamNotFound() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.getTeamForEdit(1L));
        assertEquals("Team not found", exception.getMessage());
    }

    @Test
    void testGetTeamForEdit_Success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findByTeamIdAndRole(1L, Role.MANAGER)).thenReturn(List.of(manager));

        EditTeamRequest result = adminService.getTeamForEdit(1L);

        assertEquals("Test Team", result.teamName());
        assertEquals("Rahul Sharma", result.managerName());
        assertEquals("rahul", result.managerUsername());
    }

    @Test
    void testGetTeamForEdit_NoManager() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findByTeamIdAndRole(1L, Role.MANAGER)).thenReturn(Collections.emptyList());

        EditTeamRequest result = adminService.getTeamForEdit(1L);

        assertEquals("Test Team", result.teamName());
        assertEquals("", result.managerName());
        assertEquals("", result.managerUsername());
    }

    @Test
    void testEditTeam_TeamNotFound() {
        EditTeamRequest request = new EditTeamRequest("New Name", "Manager Name", "username");
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.editTeam(1L, request));
        assertEquals("Team not found", exception.getMessage());
    }

    @Test
    void testEditTeam_TeamNameExists() {
        EditTeamRequest request = new EditTeamRequest("New Name", "Manager Name", "username");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByTeamName("New Name")).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.editTeam(1L, request));
        assertEquals("Team name already exists", exception.getMessage());
    }

    @Test
    void testEditTeam_UsernameExists() {
        EditTeamRequest request = new EditTeamRequest("Test Team", "Rahul Sharma", "newusername");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findByTeamIdAndRole(1L, Role.MANAGER)).thenReturn(List.of(manager));
        when(userRepository.existsByUsername("newusername")).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.editTeam(1L, request));
        assertEquals("Manager DAS ID already exists", exception.getMessage());
    }

    @Test
    void testEditTeam_Success() {
        EditTeamRequest request = new EditTeamRequest("Updated Team", "Updated Manager", "newusername");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByTeamName("Updated Team")).thenReturn(false);
        when(userRepository.findByTeamIdAndRole(1L, Role.MANAGER)).thenReturn(List.of(manager));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);

        adminService.editTeam(1L, request);

        assertEquals("Updated Team", team.getTeamName());
        assertEquals("Updated Manager", manager.getFullName());
        assertEquals("newusername", manager.getUsername());
    }

    @Test
    void testDeactivateTeam_NotFound() {
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.deactivateTeam(1L));
        assertEquals("Team not found", exception.getMessage());
    }

    @Test
    void testDeactivateTeam_Success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        // Mock getManagerForTeam returning manager
        when(userRepository.findByTeamIdAndRole(1L, Role.MANAGER)).thenReturn(List.of(manager));

        // Mock teamManager deactivation
        TeamManager tmRecord = new TeamManager();
        tmRecord.setActive(true);
        when(teamManagerRepository.findByTeamIdAndActiveTrue(1L)).thenReturn(Optional.of(tmRecord));

        User employeeUser = new User();
        employeeUser.setId(3L);
        employeeUser.setFullName("Amit Khan");
        employeeUser.setActive(true);

        List<User> members = new ArrayList<>(List.of(employeeUser));
        when(userRepository.findByTeamId(1L)).thenReturn(members);

        // Setup session deactivation mocks for manager (id 2L) and employee (id 3L)
        CustomUserDetails mockManagerDetails = mock(CustomUserDetails.class);
        when(mockManagerDetails.getId()).thenReturn(manager.getId()); // 2L

        CustomUserDetails mockEmployeeDetails = mock(CustomUserDetails.class);
        when(mockEmployeeDetails.getId()).thenReturn(employeeUser.getId()); // 3L

        List<Object> mockPrincipals = List.of(mockManagerDetails, mockEmployeeDetails);
        when(sessionRegistry.getAllPrincipals()).thenReturn(mockPrincipals);

        SessionInformation mockManagerSession = mock(SessionInformation.class);
        when(sessionRegistry.getAllSessions(mockManagerDetails, false)).thenReturn(List.of(mockManagerSession));

        SessionInformation mockEmployeeSession = mock(SessionInformation.class);
        when(sessionRegistry.getAllSessions(mockEmployeeDetails, false)).thenReturn(List.of(mockEmployeeSession));

        adminService.deactivateTeam(1L);

        assertFalse(team.isActive());
        assertFalse(manager.isActive());
        assertFalse(tmRecord.isActive());
        assertFalse(employeeUser.isActive());

        verify(mockManagerSession, times(1)).expireNow();
        verify(mockEmployeeSession, times(1)).expireNow();
    }

    @Test
    void testResetManagerPassword_NotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.resetManagerPassword(2L));
        assertEquals("Manager not found", exception.getMessage());
    }

    @Test
    void testResetManagerPassword_NotAManager() {
        manager.getRoles().clear();
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> adminService.resetManagerPassword(2L));
        assertEquals("User is not a manager", exception.getMessage());
    }

    @Test
    void testResetManagerPassword_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(passwordEncoder.encode("rahuls@123")).thenReturn("encodedDefaultPass");

        adminService.resetManagerPassword(2L);

        assertEquals("encodedDefaultPass", manager.getPassword());
        assertFalse(manager.isPasswordChanged());
    }
}
