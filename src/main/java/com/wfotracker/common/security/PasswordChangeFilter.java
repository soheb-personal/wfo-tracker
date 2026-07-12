package com.wfotracker.common.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wfotracker.domain.repository.EmployeeMembershipRepository;
import com.wfotracker.domain.repository.TeamManagerRepository;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordChangeFilter extends OncePerRequestFilter {

    private static final String LOGOUT_REDIRECT = "/login?logout";

    private final UserRepository userRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final EmployeeMembershipRepository employeeMembershipRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null
                && (uri.startsWith("/css/")
                        || uri.startsWith("/js/")
                        || uri.startsWith("/images/")
                        || uri.equals("/favicon.ico"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String uri = request.getRequestURI();

        if (isUserAuthenticated(authentication) && uri != null) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (shouldInvalidateSession(userDetails, uri)) {
                handleLogoutRedirect(request, response);
                return;
            }

            if (shouldForcePasswordChange(userDetails, uri)) {
                response.sendRedirect("/change-password");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUserAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails;
    }

    private boolean shouldInvalidateSession(CustomUserDetails userDetails, String uri) {
        if (userDetails == null || userDetails.getId() == null || uri == null) {
            return true;
        }

        // 1. Real-time active status check
        boolean isActive = userRepository
                .findById(userDetails.getId())
                .map(com.wfotracker.domain.entity.User::isActive)
                .orElse(false);
        if (!isActive) {
            return true;
        }

        // 2. Real-time manager active assignment check
        if (uri.startsWith("/manager/")) {
            return teamManagerRepository
                    .findByManagerIdAndActiveTrue(userDetails.getId())
                    .isEmpty();
        }

        // 3. Real-time employee active membership check
        if (uri.startsWith("/employee/")) {
            return employeeMembershipRepository
                    .findByEmployeeIdAndActiveTrue(userDetails.getId())
                    .isEmpty();
        }

        return false;
    }

    private boolean shouldForcePasswordChange(CustomUserDetails userDetails, String uri) {
        if (userDetails == null || uri == null) {
            return false;
        }
        return !userDetails.isPasswordChanged() && !"/change-password".equals(uri) && !"/logout".equals(uri);
    }

    private void handleLogoutRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(LOGOUT_REDIRECT);
    }
}
