package com.wfotracker.domain.repository;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.domain.entity.EmployeeMembership;
import com.wfotracker.domain.entity.Group;
import com.wfotracker.domain.entity.Role;
import com.wfotracker.domain.entity.Team;
import com.wfotracker.domain.entity.TeamManager;
import com.wfotracker.domain.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class RepositoryPhase1Test {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamManagerRepository teamManagerRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EmployeeMembershipRepository employeeMembershipRepository;

    @Test
    void testCreateUserWithMultipleRoles() {
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN_TEST");
        adminRole = roleRepository.save(adminRole);

        Role managerRole = new Role();
        managerRole.setName("ROLE_MANAGER_TEST");
        managerRole = roleRepository.save(managerRole);

        User user = new User();
        user.setFullName("Multi Role User");
        user.setUsername("multi_role");
        user.setPassword("password");
        user.getRoles().add(adminRole);
        user.getRoles().add(managerRole);

        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        assertEquals(2, savedUser.getRoles().size());

        Optional<User> fetchedUserOpt = userRepository.findByUsername("multi_role");
        assertTrue(fetchedUserOpt.isPresent());
        User fetchedUser = fetchedUserOpt.get();
        assertTrue(fetchedUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN_TEST")));
        assertTrue(fetchedUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_MANAGER_TEST")));
    }

    @Test
    void testUniqueUsernameEnforced() {
        User user1 = new User();
        user1.setFullName("User One");
        user1.setUsername("duplicate_uname");
        user1.setPassword("password");
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setFullName("User Two");
        user2.setUsername("duplicate_uname");
        user2.setPassword("password");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    userRepository.saveAndFlush(user2);
                },
                "Username uniqueness must be enforced by DB constraints");
    }

    @Test
    void testTeamManagerUniqueness() {
        Team team1 = new Team();
        team1.setTeamName("Finance Team");
        team1 = teamRepository.save(team1);

        Team team2 = new Team();
        team2.setTeamName("HR Team");
        team2 = teamRepository.save(team2);

        User manager1 = new User();
        manager1.setFullName("Manager One");
        manager1.setUsername("mgr1");
        manager1.setPassword("password");
        manager1 = userRepository.save(manager1);

        User manager2 = new User();
        manager2.setFullName("Manager Two");
        manager2.setUsername("mgr2");
        manager2.setPassword("password");
        userRepository.save(manager2);

        // Assign manager1 to team1 (valid)
        TeamManager tm1 = new TeamManager();
        tm1.setTeam(team1);
        tm1.setManager(manager1);
        teamManagerRepository.saveAndFlush(tm1);

        // Assign manager1 to team2 (violates unique manager_id constraint)
        TeamManager tm2 = new TeamManager();
        tm2.setTeam(team2);
        tm2.setManager(manager1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    teamManagerRepository.saveAndFlush(tm2);
                },
                "A manager cannot be assigned to more than one team");
    }

    @Test
    void testCustomGroupsAndEmployeeMemberships() {
        Team team = new Team();
        team.setTeamName("Product Team");
        team = teamRepository.save(team);

        Group group = new Group();
        group.setTeam(team);
        group.setGroupName("Frontend Squad");
        group = groupRepository.save(group);

        User manager = new User();
        manager.setFullName("Team Manager");
        manager.setUsername("manager1");
        manager.setPassword("password");
        manager = userRepository.save(manager);

        User employee = new User();
        employee.setFullName("Jane Doe");
        employee.setUsername("janedoe");
        employee.setPassword("password");
        employee = userRepository.save(employee);

        EmployeeMembership membership = new EmployeeMembership();
        membership.setEmployee(employee);
        membership.setTeam(team);
        membership.setManager(manager);
        membership.setGroup(group);
        membership.setActive(true);
        membership = employeeMembershipRepository.save(membership);

        assertNotNull(membership.getId());

        Optional<EmployeeMembership> activeMembership =
                employeeMembershipRepository.findByEmployeeIdAndActiveTrue(employee.getId());
        assertTrue(activeMembership.isPresent());
        assertEquals(team.getId(), activeMembership.get().getTeam().getId());
        assertEquals(group.getId(), activeMembership.get().getGroup().getId());
    }
}
