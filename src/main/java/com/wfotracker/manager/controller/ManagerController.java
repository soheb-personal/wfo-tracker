package com.wfotracker.manager.controller;

import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.compliance.service.ComplianceService;
import com.wfotracker.domain.entity.User;
import com.wfotracker.manager.dto.AddEmployeeRequest;
import com.wfotracker.manager.dto.EditEmployeeRequest;
import com.wfotracker.manager.dto.EmployeeComplianceDto;
import com.wfotracker.manager.dto.MonthlyConfigRequest;
import com.wfotracker.manager.service.ManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;
    private final ComplianceService complianceService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Integer month,
                            @RequestParam(required = false) Integer year,
                            @AuthenticationPrincipal CustomUserDetails userDetails, 
                            Model model) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();

        List<User> employees = managerService.getEmployeesForManager(userDetails.getId());
        List<EmployeeComplianceDto> complianceList = employees.stream()
                .map(emp -> complianceService.getComplianceForEmployee(emp, m, y))
                .collect(Collectors.toList());

        model.addAttribute("employeesCompliance", complianceList);
        model.addAttribute("currentMonth", m);
        model.addAttribute("currentYear", y);
        return "manager-dashboard";
    }

    @GetMapping("/employee/add")
    public String showAddEmployeeForm(Model model) {
        model.addAttribute("addEmployeeRequest", new AddEmployeeRequest(""));
        return "employee-form";
    }

    @PostMapping("/employee/add")
    public String addEmployee(@Valid @ModelAttribute("addEmployeeRequest") AddEmployeeRequest request,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "employee-form";
        }

        try {
            managerService.addEmployee(userDetails.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Employee added successfully.");
            return "redirect:/manager/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "employee-form";
        }
    }

    @GetMapping("/employee/list")
    public String listEmployees(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("employees", managerService.getEmployeesForManager(userDetails.getId()));
        return "employee-list";
    }

    @GetMapping("/employee/edit/{id}")
    public String showEditEmployeeForm(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("editEmployeeRequest", managerService.getEmployeeForEdit(userDetails.getId(), id));
        model.addAttribute("employeeId", id);
        return "employee-edit";
    }

    @PostMapping("/employee/edit/{id}")
    public String editEmployee(@PathVariable Long id,
                               @Valid @ModelAttribute("editEmployeeRequest") EditEmployeeRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("employeeId", id);
            return "employee-edit";
        }

        try {
            managerService.editEmployee(userDetails.getId(), id, request);
            redirectAttributes.addFlashAttribute("success", "Employee updated successfully.");
            return "redirect:/manager/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("employeeId", id);
            return "employee-edit";
        }
    }

    @PostMapping("/employee/deactivate/{id}")
    public String deactivateEmployee(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            managerService.deactivateEmployee(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Employee deactivated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/dashboard";
    }

    @PostMapping("/employee/reset-password/{id}")
    public String resetEmployeePassword(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            managerService.resetEmployeePassword(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute("success", "Employee password reset successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/manager/dashboard";
    }

    @GetMapping("/employee/configure/{id}")
    public String showConfigureEmployeeForm(@PathVariable Long id,
                                            @RequestParam(required = false) Integer month,
                                            @RequestParam(required = false) Integer year,
                                            @AuthenticationPrincipal CustomUserDetails userDetails,
                                            Model model) {
        int m = month != null ? month : LocalDate.now().getMonthValue();
        int y = year != null ? year : LocalDate.now().getYear();

        var config = managerService.getMonthlyConfig(userDetails.getId(), id, m, y);
        
        MonthlyConfigRequest request = new MonthlyConfigRequest(config.getLeaves(), config.getPublicHolidays(), config.getExceptionDays(), m, y);
        model.addAttribute("monthlyConfigRequest", request);
        model.addAttribute("employeeId", id);
        return "employee-config";
    }

    @PostMapping("/employee/configure/{id}")
    public String configureEmployee(@PathVariable Long id,
                                    @Valid @ModelAttribute("monthlyConfigRequest") MonthlyConfigRequest request,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("employeeId", id);
            return "employee-config";
        }

        try {
            managerService.configureMonthly(userDetails.getId(), id, request);
            redirectAttributes.addFlashAttribute("success", "Monthly configuration updated.");
            return "redirect:/manager/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("employeeId", id);
            return "employee-config";
        }
    }
}
