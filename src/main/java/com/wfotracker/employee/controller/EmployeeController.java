package com.wfotracker.employee.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.compliance.service.ComplianceService;
import com.wfotracker.employee.service.EmployeeService;
import com.wfotracker.manager.dto.EmployeeComplianceDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ComplianceService complianceService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        LocalDate now = LocalDate.now();
        EmployeeComplianceDto compliance =
                complianceService.getComplianceForEmployee(userDetails.getUser(), now.getMonthValue(), now.getYear());

        model.addAttribute("compliance", compliance);
        model.addAttribute("isCheckedIn", employeeService.isCheckedInToday(userDetails.getId()));
        model.addAttribute("isCheckedOut", employeeService.isCheckedOutToday(userDetails.getId()));

        return "employee-dashboard";
    }

    @PostMapping("/checkin")
    public String checkIn(
            @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            employeeService.checkIn(userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Checked in successfully.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/dashboard";
    }

    @PostMapping("/checkout")
    public String checkOut(
            @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            employeeService.checkOut(userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Checked out successfully.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employee/dashboard";
    }

    @GetMapping("/history")
    public String history(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();

        model.addAttribute("history", employeeService.getAttendanceHistory(userDetails.getId(), m, y));
        model.addAttribute("currentMonth", m);
        model.addAttribute("currentYear", y);
        return "attendance-history";
    }
}
