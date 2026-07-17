package com.wfotracker.common.security;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.EmployeeMembershipRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordChangeFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private EmployeeMembershipRepository employeeMembershipRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private PasswordChangeFilter filter;

    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testShouldNotFilter_StaticAssets() {
        when(request.getRequestURI()).thenReturn("/css/bootstrap.css");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/js/main.js");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/favicon.ico");
        assertTrue(filter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/manager/dashboard");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void testDoFilterInternal_NotAuthenticated() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/manager/dashboard");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ForcePasswordChange() throws ServletException, IOException {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setFullName("John Doe");
        user.setPasswordChanged(false);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(request.getRequestURI()).thenReturn("/home");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/change-password");
    }

    @Test
    void testDoFilterInternal_InactiveUser_Logout() throws ServletException, IOException {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setActive(false);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(request.getRequestURI()).thenReturn("/manager/dashboard");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        filter.doFilterInternal(request, response, filterChain);

        verify(session).invalidate();
        verify(response).sendRedirect("/login?logout");
    }

    @Test
    void testDoFilterInternal_InactiveManager() throws ServletException, IOException {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setActive(true);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(request.getRequestURI()).thenReturn("/manager/dashboard");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(teamManagerRepository.findByManagerIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/login?logout");
    }

    @Test
    void testDoFilterInternal_InactiveEmployee() throws ServletException, IOException {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setActive(true);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(auth);
        when(request.getRequestURI()).thenReturn("/employee/dashboard");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeMembershipRepository.findByEmployeeIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendRedirect("/login?logout");
    }
}
