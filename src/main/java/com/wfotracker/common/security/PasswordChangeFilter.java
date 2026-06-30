package com.wfotracker.common.security;

import com.wfotracker.common.constants.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PasswordChangeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String uri = request.getRequestURI();

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));

            if (!isAdmin && !userDetails.isPasswordChanged()) {
                if (!uri.equals("/change-password") && !uri.equals("/logout") && !uri.startsWith("/css/") && !uri.startsWith("/js/")) {
                    response.sendRedirect("/change-password");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
