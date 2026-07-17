package com.wfotracker.common.security;

import java.util.Collections;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.wfotracker.domain.entity.Role;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void testLoadUserByUsername_NoRoleSelected() {
        when(request.getParameter("role")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername("john");
        });
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        when(request.getParameter("role")).thenReturn("ROLE_EMPLOYEE");
        when(userRepository.findByUsernameAndActiveTrue("john")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername("john");
        });
    }

    @Test
    void testLoadUserByUsername_RoleMismatch() {
        when(request.getParameter("role")).thenReturn("ROLE_EMPLOYEE");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setFullName("John Doe");

        Role managerRole = new Role();
        managerRole.setName("ROLE_MANAGER");
        user.setRoles(Collections.singleton(managerRole));

        when(userRepository.findByUsernameAndActiveTrue("john")).thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername("john");
        });
    }

    @Test
    void testLoadUserByUsername_Success() {
        when(request.getParameter("role")).thenReturn("ROLE_EMPLOYEE");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setFullName("John Doe");

        Role employeeRole = new Role();
        employeeRole.setName("ROLE_EMPLOYEE");
        user.setRoles(Collections.singleton(employeeRole));

        when(userRepository.findByUsernameAndActiveTrue("john")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("john");

        assertNotNull(details);
        assertEquals("john", details.getUsername());
    }
}
