package com.wfotracker.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.wfotracker.domain.entity.Role;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MultiRoleAuthenticationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private Role roleAdmin;
    private Role roleManager;
    private Role roleEmployee;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Ensure roles are seeded
        roleAdmin = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_ADMIN");
            return roleRepository.save(r);
        });

        roleManager = roleRepository.findByName("ROLE_MANAGER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_MANAGER");
            return roleRepository.save(r);
        });

        roleEmployee = roleRepository.findByName("ROLE_EMPLOYEE").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_EMPLOYEE");
            return roleRepository.save(r);
        });

        // 1. Create standard Admin user
        User admin = new User();
        admin.setFullName("Test Admin");
        admin.setUsername("testadmin");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.getRoles().add(roleAdmin);
        userRepository.save(admin);

        // 2. Create standard Manager user
        User manager = new User();
        manager.setFullName("Test Manager");
        manager.setUsername("testmanager");
        manager.setPassword(passwordEncoder.encode("managerpass"));
        manager.getRoles().add(roleManager);
        userRepository.save(manager);

        // 3. Create Multi-Role user (Manager + Employee)
        User multiUser = new User();
        multiUser.setFullName("Multi User");
        multiUser.setUsername("multiuser");
        multiUser.setPassword(passwordEncoder.encode("multipass"));
        multiUser.getRoles().add(roleManager);
        multiUser.getRoles().add(roleEmployee);
        userRepository.save(multiUser);

        // 4. Create standard Employee user (with password changed)
        employeeUser = new User();
        employeeUser.setFullName("Test Employee");
        employeeUser.setUsername("testemployee");
        employeeUser.setPassword(passwordEncoder.encode("employeepass"));
        employeeUser.getRoles().add(roleEmployee);
        employeeUser.setPasswordChanged(true);
        userRepository.save(employeeUser);
    }

    @Test
    void testAdminLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "testadmin")
                        .param("password", "adminpass")
                        .param("role", "ROLE_ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void testManagerLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "testmanager")
                        .param("password", "managerpass")
                        .param("role", "ROLE_MANAGER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));
    }

    @Test
    void testLoginFailsWhenRoleSelectionIsOmitted() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "testadmin")
                        .param("password", "adminpass")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void testLoginFailsWhenUserDoesNotHaveSelectedRole() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "testmanager")
                        .param("password", "managerpass")
                        .param("role", "ROLE_ADMIN") // manager has only ROLE_MANAGER
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void testMultiRoleUserCanLoginAsManager() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "multiuser")
                        .param("password", "multipass")
                        .param("role", "ROLE_MANAGER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager/dashboard"));
    }

    @Test
    void testMultiRoleUserCanLoginAsEmployee() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "multiuser")
                        .param("password", "multipass")
                        .param("role", "ROLE_EMPLOYEE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));
    }

    @Test
    void testMultiRoleUserFailsLoginWithWrongRole() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "multiuser")
                        .param("password", "multipass")
                        .param("role", "ROLE_ADMIN") // multiuser does not have ROLE_ADMIN
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void testEmployeeCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user(new com.wfotracker.common.security.CustomUserDetails(employeeUser))))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }
}
