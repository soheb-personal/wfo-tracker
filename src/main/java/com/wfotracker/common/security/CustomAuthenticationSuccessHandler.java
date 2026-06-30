package com.wfotracker.common.security;

import com.wfotracker.common.constants.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        if (!userDetails.isPasswordChanged() && !authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_" + Role.ADMIN.name()))) {
            response.sendRedirect("/change-password");
            return;
        }

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if ("ROLE_MANAGER".equals(auth.getAuthority())) {
                response.sendRedirect("/manager/dashboard");
                return;
            } else if ("ROLE_EMPLOYEE".equals(auth.getAuthority())) {
                response.sendRedirect("/employee/dashboard");
                return;
            }
        }

        response.sendRedirect("/");
    }
}
