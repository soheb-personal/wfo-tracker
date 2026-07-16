package com.wfotracker.manager.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.EmployeeMembership;
import com.wfotracker.domain.entity.Group;
import com.wfotracker.domain.entity.MonthlyConfiguration;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.EmployeeMembershipRepository;
import com.wfotracker.domain.repository.GroupRepository;
import com.wfotracker.domain.repository.MonthlyConfigurationRepository;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.UserRepository;
import com.wfotracker.manager.dto.AddEmployeeRequest;
import com.wfotracker.manager.dto.EditEmployeeRequest;
import com.wfotracker.manager.dto.MonthlyConfigRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MonthlyConfigurationRepository monthlyConfigRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private EmployeeMembershipRepository employeeMembershipRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private ManagerService managerService;

    private User manager;
    private User employee;
    private Team team;
    private TeamManager teamManager;
    private EmployeeMembership membership;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setFullName("John Manager");
        manager.setUsername("john");

        employee = new User();
        employee.setId(2L);
        employee.setFullName("Jane Employee");
        employee.setUsername("jane");

        team = new Team();
        team.setId(10L);
        team.setTeamName("Engineering");

        teamManager = new TeamManager();
        teamManager.setManager(manager);
        teamManager.setTeam(team);

        membership = new EmployeeMembership();
        membership.setEmployee(employee);
        membership.setManager(manager);
        membership.setTeam(team);
        membership.setActive(true);
    }

    @Test
    void testGetEmployeesForManager() {
        when(userRepository.findByManagerIdAndRoleAndActiveTrue(1L, Role.EMPLOYEE))
                .thenReturn(List.of(employee));

        List<User> employees = managerService.getEmployeesForManager(1L);
        assertEquals(1, employees.size());
        assertEquals(employee.getUsername(), employees.get(0).getUsername());
    }

    @Test
    void testAddEmployee_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.of(teamManager));
        when(userRepository.findByUsername("jane")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");

        com.wfotracker.domain.entity.Role empRole = new com.wfotracker.domain.entity.Role();
        empRole.setName("ROLE_EMPLOYEE");
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(empRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddEmployeeRequest request = new AddEmployeeRequest("Jane Employee", "jane");
        managerService.addEmployee(1L, request);

        verify(userRepository).save(any(User.class));
        verify(employeeMembershipRepository).save(any(EmployeeMembership.class));
    }

    @Test
    void testAddEmployee_DasIdAlreadyExistsAndAlreadyActiveEmployee() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.of(teamManager));

        com.wfotracker.domain.entity.Role empRole = new com.wfotracker.domain.entity.Role();
        empRole.setName("ROLE_EMPLOYEE");
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(empRole));

        when(userRepository.findByUsername("jane")).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(employee.getId()))
                .thenReturn(Optional.of(membership));

        AddEmployeeRequest request = new AddEmployeeRequest("Jane Employee", "jane");
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> managerService.addEmployee(1L, request));
        assertEquals("Employee is already active in another team", exception.getMessage());
    }

    @Test
    void testAddEmployee_ManagerNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AddEmployeeRequest addEmployeeRequest = new AddEmployeeRequest("Jane", "jane");

        assertThrows(IllegalArgumentException.class, () -> managerService.addEmployee(1L, addEmployeeRequest));
    }

    @Test
    void testAddEmployee_TeamManagerNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        AddEmployeeRequest addEmployeeRequest = new AddEmployeeRequest("Jane", "jane");
        assertThrows(IllegalArgumentException.class, () -> managerService.addEmployee(1L, addEmployeeRequest));
    }

    @Test
    void testGetEmployeeForEdit_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));

        EditEmployeeRequest request = managerService.getEmployeeForEdit(1L, 2L);
        assertEquals("Jane Employee", request.employeeName());
        assertEquals("jane", request.username());
    }

    @Test
    void testGetEmployeeForEdit_Unauthorized() {
        EmployeeMembership otherMembership = new EmployeeMembership();
        otherMembership.setEmployee(employee);
        User differentManager = new User();
        differentManager.setId(99L);
        otherMembership.setManager(differentManager);
        otherMembership.setTeam(team);
        otherMembership.setActive(true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(otherMembership));

        assertThrows(IllegalArgumentException.class, () -> managerService.getEmployeeForEdit(1L, 2L));
    }

    @Test
    void testEditEmployee_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);

        EditEmployeeRequest request = new EditEmployeeRequest("Jane Edited", "newusername");
        managerService.editEmployee(1L, 2L, request);

        assertEquals("Jane Edited", employee.getFullName());
        assertEquals("newusername", employee.getUsername());
    }

    @Test
    void testEditEmployee_UsernameExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));
        when(userRepository.existsByUsername("otheruser")).thenReturn(true);

        EditEmployeeRequest request = new EditEmployeeRequest("Jane", "otheruser");
        assertThrows(IllegalArgumentException.class, () -> managerService.editEmployee(1L, 2L, request));
    }

    @Test
    void testDeactivateEmployee() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));

        managerService.deactivateEmployee(1L, 2L);
        assertFalse(employee.isActive());
        assertFalse(membership.isActive());
    }

    @Test
    void testResetEmployeePassword() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));
        when(passwordEncoder.encode(any())).thenReturn("newPass");

        managerService.resetEmployeePassword(1L, 2L);
        assertEquals("newPass", employee.getPassword());
    }

    @Test
    void testConfigureMonthly_CreateNew() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));
        when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(2L, 7, 2026))
                .thenReturn(Optional.empty());

        MonthlyConfigRequest request = new MonthlyConfigRequest(2, 1, 0, 1, 7, 2026);
        managerService.configureMonthly(1L, 2L, request);

        verify(monthlyConfigRepository).save(any(MonthlyConfiguration.class));
    }

    @Test
    void testGetMonthlyConfig_Existing() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));

        MonthlyConfiguration config = new MonthlyConfiguration();
        when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(2L, 7, 2026))
                .thenReturn(Optional.of(config));

        MonthlyConfiguration result = managerService.getMonthlyConfig(1L, 2L, 7, 2026);
        assertNotNull(result);
    }

    @Test
    void testGetMonthlyConfig_Default() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(2L)).thenReturn(Optional.of(membership));
        when(monthlyConfigRepository.findByEmployeeIdAndMonthAndYear(2L, 7, 2026))
                .thenReturn(Optional.empty());

        MonthlyConfiguration result = managerService.getMonthlyConfig(1L, 2L, 7, 2026);
        assertNotNull(result);
        assertEquals(7, result.getMonth());
        assertEquals(2026, result.getYear());
    }

    @Test
    void testGetGroupsForManager() {
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.of(teamManager));
        when(groupRepository.findByTeamIdAndActiveTrue(10L)).thenReturn(Collections.emptyList());

        List<Group> groups = managerService.getGroupsForManager(1L);
        assertNotNull(groups);
    }

    @Test
    void testCreateGroup_Success() {
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.of(teamManager));
        when(groupRepository.existsByTeamIdAndGroupNameAndActiveTrue(10L, "QA")).thenReturn(false);

        managerService.createGroup(1L, "QA");
        verify(groupRepository).save(any(Group.class));
    }

    @Test
    void testCreateGroup_NameEmpty() {
        assertThrows(IllegalArgumentException.class, () -> managerService.createGroup(1L, ""));
    }

    @Test
    void testCreateGroup_AlreadyExists() {
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.of(teamManager));
        when(groupRepository.existsByTeamIdAndGroupNameAndActiveTrue(10L, "QA")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> managerService.createGroup(1L, "QA"));
    }

    @Test
    void testDeleteGroup_Success() {
        Group group = new Group();
        group.setId(30L);
        group.setTeam(team);
        group.setActive(true);

        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.of(teamManager));
        when(groupRepository.findById(30L)).thenReturn(Optional.of(group));
        when(employeeMembershipRepository.findByGroupIdAndActiveTrue(30L)).thenReturn(List.of(membership));

        managerService.deleteGroup(1L, 30L);

        assertNull(membership.getGroup());
        verify(employeeMembershipRepository).saveAll(anyList());
        verify(groupRepository).delete(group);
    }

    @Test
    void testDeleteDeactivatedEmployee_Success() {
        membership.setActive(false); // Scope deactivated
        when(employeeMembershipRepository.findByEmployeeId(2L)).thenReturn(List.of(membership));

        managerService.deleteDeactivatedEmployee(1L, 2L);
        verify(employeeMembershipRepository).delete(membership);
    }

    @Test
    void testDeleteDeactivatedEmployee_ActiveFails() {
        when(employeeMembershipRepository.findByEmployeeId(2L)).thenReturn(List.of(membership)); // active is true

        assertThrows(IllegalArgumentException.class, () -> managerService.deleteDeactivatedEmployee(1L, 2L));
    }
}
