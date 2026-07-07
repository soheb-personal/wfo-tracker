package com.wfotracker.common.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        String selectedRole = request.getParameter("role");
        if (selectedRole != null && !selectedRole.trim().isEmpty()) {
            String roleStr = selectedRole.trim().toUpperCase();
            if ("ROLE_ADMIN".equals(roleStr)) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if ("ROLE_MANAGER".equals(roleStr)) {
                response.sendRedirect("/manager/dashboard");
                return;
            } else if ("ROLE_EMPLOYEE".equals(roleStr)) {
                response.sendRedirect("/employee/dashboard");
                return;
            }
        }

        response.sendRedirect("/");
    }
}
