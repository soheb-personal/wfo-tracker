package com.wfotracker.auth.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.wfotracker.auth.dto.ChangePasswordRequest;
import com.wfotracker.auth.service.AuthService;
import com.wfotracker.common.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChangePasswordController {

    private static final String TEMPLATE_CHANGE_PASSWORD = "change-password";
    private static final String ATTR_CHANGE_PASSWORD_REQUEST = "changePasswordRequest";

    private final AuthService authService;

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute(ATTR_CHANGE_PASSWORD_REQUEST, new ChangePasswordRequest("", ""));
        return TEMPLATE_CHANGE_PASSWORD;
    }

    @PostMapping("/change-password")
    public String changePassword(
            @Valid @ModelAttribute(ATTR_CHANGE_PASSWORD_REQUEST) ChangePasswordRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest,
            Model model) {

        if (bindingResult.hasErrors()) {
            return TEMPLATE_CHANGE_PASSWORD;
        }

        try {
            authService.changePassword(userDetails, request);
            httpRequest.logout();
        } catch (IllegalArgumentException | ServletException e) {
            model.addAttribute("error", e.getMessage());
            return TEMPLATE_CHANGE_PASSWORD;
        }

        return "redirect:/login?passwordChanged";
    }
}
