package com.wfotracker.common.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.wfotracker.common.constants.Role;
import com.wfotracker.common.security.CustomUserDetails;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()))) {
                return "redirect:/admin/dashboard";
            } else if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.MANAGER.name()))) {
                return "redirect:/manager/dashboard";
            } else {
                return "redirect:/employee/dashboard";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
