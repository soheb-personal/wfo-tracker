package com.wfotracker.manager.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import jakarta.validation.Valid;

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

import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.compliance.service.ComplianceService;
import com.wfotracker.domain.entity.Attendance;
import com.wfotracker.domain.entity.EmployeeMembership;
import com.wfotracker.domain.entity.Group;
import com.wfotracker.domain.entity.User;
import com.wfotracker.manager.dto.AddEmployeeRequest;
import com.wfotracker.manager.dto.EditEmployeeRequest;
import com.wfotracker.manager.dto.EmployeeComplianceDto;
import com.wfotracker.manager.dto.MonthlyConfigRequest;
import com.wfotracker.manager.service.ManagerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private static final String REDIRECT_MANAGER_DASHBOARD = "redirect:/manager/dashboard";
    private static final String REDIRECT_EMPLOYEE_LIST = "redirect:/manager/employee/list";
    private static final String REDIRECT_GROUP_LIST = "redirect:/manager/group/list";

    private static final String TEMPLATE_EMPLOYEE_FORM = "employee-form";
    private static final String TEMPLATE_EMPLOYEE_EDIT = "employee-edit";
    private static final String TEMPLATE_EMPLOYEE_CONFIG = "employee-config";
    private static final String TEMPLATE_GROUP_FORM = "group-form";
    private static final String TEMPLATE_GROUP_LIST = "group-list";
    private static final String TEMPLATE_EMPLOYEE_LIST = "employee-list";
    private static final String TEMPLATE_ATTENDANCE_HISTORY = "attendance-history";

    private static final String ATTR_EMPLOYEE_ID = "employeeId";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_ADD_EMPLOYEE_REQUEST = "addEmployeeRequest";
    private static final String ATTR_EDIT_EMPLOYEE_REQUEST = "editEmployeeRequest";
    private static final String ATTR_MONTHLY_CONFIG_REQUEST = "monthlyConfigRequest";
    private static final String GROUP_NAME = "groupName";
    private static final String GROUPS = "groups";

    private final ManagerService managerService;
    private final ComplianceService complianceService;

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        int m = month != null ? month : LocalDate.now(ZoneId.of("UTC")).getMonthValue();
        int y = year != null ? year : LocalDate.now(ZoneId.of("UTC")).getYear();

        List<EmployeeMembership> memberships = managerService.getMembershipsForManager(userDetails.getId(), groupId);

        List<EmployeeComplianceDto> complianceList = memberships.stream()
                .map(membership -> complianceService.getComplianceForEmployee(
                        membership.getEmployee(), membership.isActive(), m, y))
                .toList();

        List<Group> activeGroups = managerService.getGroupsForManager(userDetails.getId());
        boolean showDefaultGroupOption = managerService.getMembershipsForManager(userDetails.getId(), null).stream()
                .anyMatch(membership -> membership.getGroup() == null);

        model.addAttribute("employeesCompliance", complianceList);
        model.addAttribute("currentMonth", m);
        model.addAttribute("currentYear", y);
        model.addAttribute(GROUPS, activeGroups);
        model.addAttribute("showDefault", showDefaultGroupOption);
        model.addAttribute("groupIdSelected", groupId);
        model.addAttribute("activeTab", "dashboard");

        return "manager-dashboard";
    }

    @GetMapping("/employee/add")
    public String showAddEmployeeForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute(ATTR_ADD_EMPLOYEE_REQUEST, new AddEmployeeRequest("", "", null));
        model.addAttribute(GROUPS, managerService.getGroupsForManager(userDetails.getId()));
        model.addAttribute("activeTab", "employees");
        return TEMPLATE_EMPLOYEE_FORM;
    }

    @PostMapping("/employee/add")
    public String addEmployee(
            @Valid @ModelAttribute(ATTR_ADD_EMPLOYEE_REQUEST) AddEmployeeRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(GROUPS, managerService.getGroupsForManager(userDetails.getId()));
            model.addAttribute("activeTab", "employees");
            return TEMPLATE_EMPLOYEE_FORM;
        }

        try {
            managerService.addEmployee(userDetails.getId(), request);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Employee added successfully.");
            return REDIRECT_MANAGER_DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTR_ERROR, e.getMessage());
            model.addAttribute(GROUPS, managerService.getGroupsForManager(userDetails.getId()));
            model.addAttribute("activeTab", "employees");
            return TEMPLATE_EMPLOYEE_FORM;
        }
    }

    @GetMapping("/employee/list")
    public String listEmployees(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<EmployeeMembership> memberships = managerService.getMembershipsForManager(userDetails.getId(), null);
        model.addAttribute("memberships", memberships);

        List<User> employees = managerService.getEmployeesForManager(userDetails.getId());
        model.addAttribute("employees", employees);
        model.addAttribute("activeTab", "employees");

        return TEMPLATE_EMPLOYEE_LIST;
    }

    @GetMapping("/employee/edit/{id}")
    public String showEditEmployeeForm(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute(ATTR_EDIT_EMPLOYEE_REQUEST, managerService.getEmployeeForEdit(userDetails.getId(), id));
        model.addAttribute(ATTR_EMPLOYEE_ID, id);
        model.addAttribute(GROUPS, managerService.getGroupsForManager(userDetails.getId()));
        model.addAttribute("activeTab", "employees");
        return TEMPLATE_EMPLOYEE_EDIT;
    }

    @PostMapping("/employee/edit/{id}")
    public String editEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute(ATTR_EDIT_EMPLOYEE_REQUEST) EditEmployeeRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_EMPLOYEE_ID, id);
            model.addAttribute(GROUPS, managerService.getGroupsForManager(userDetails.getId()));
            model.addAttribute("activeTab", "employees");
            return TEMPLATE_EMPLOYEE_EDIT;
        }

        try {
            managerService.editEmployee(userDetails.getId(), id, request);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Employee updated successfully.");
            return REDIRECT_MANAGER_DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTR_ERROR, e.getMessage());
            model.addAttribute(ATTR_EMPLOYEE_ID, id);
            model.addAttribute(GROUPS, managerService.getGroupsForManager(userDetails.getId()));
            model.addAttribute("activeTab", "employees");
            return TEMPLATE_EMPLOYEE_EDIT;
        }
    }

    @PostMapping("/employee/deactivate/{id}")
    public String deactivateEmployee(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            managerService.deactivateEmployee(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Employee deactivated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }
        return REDIRECT_MANAGER_DASHBOARD;
    }

    @PostMapping("/employee/delete/{id}")
    public String deleteEmployee(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            managerService.deleteDeactivatedEmployee(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Employee membership permanently deleted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }
        return REDIRECT_EMPLOYEE_LIST;
    }

    @PostMapping("/employee/reset-password/{id}")
    public String resetEmployeePassword(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            managerService.resetEmployeePassword(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Employee password reset successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }
        return REDIRECT_MANAGER_DASHBOARD;
    }

    @GetMapping("/employee/configure/{id}")
    public String showConfigureEmployeeForm(
            @PathVariable Long id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        int m = month != null ? month : LocalDate.now(ZoneId.of("UTC")).getMonthValue();
        int y = year != null ? year : LocalDate.now(ZoneId.of("UTC")).getYear();

        var config = managerService.getMonthlyConfig(userDetails.getId(), id, m, y);

        MonthlyConfigRequest request = new MonthlyConfigRequest(
                config.getLeaves(),
                config.getPublicHolidays(),
                config.getExceptionDays(),
                config.getManualCheckins(),
                m,
                y);
        model.addAttribute(ATTR_MONTHLY_CONFIG_REQUEST, request);
        model.addAttribute(ATTR_EMPLOYEE_ID, id);
        model.addAttribute("activeTab", "employees");
        return TEMPLATE_EMPLOYEE_CONFIG;
    }

    @PostMapping("/employee/configure/{id}")
    public String configureEmployee(
            @PathVariable Long id,
            @Valid @ModelAttribute(ATTR_MONTHLY_CONFIG_REQUEST) MonthlyConfigRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_EMPLOYEE_ID, id);
            model.addAttribute("activeTab", "employees");
            return TEMPLATE_EMPLOYEE_CONFIG;
        }

        try {
            managerService.configureMonthly(userDetails.getId(), id, request);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Monthly configuration updated.");
            return REDIRECT_MANAGER_DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTR_ERROR, e.getMessage());
            model.addAttribute(ATTR_EMPLOYEE_ID, id);
            model.addAttribute("activeTab", "employees");
            return TEMPLATE_EMPLOYEE_CONFIG;
        }
    }

    @GetMapping("/group/list")
    public String listGroups(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Group> activeGroups = managerService.getGroupsForManager(userDetails.getId());
        model.addAttribute(GROUPS, activeGroups);
        model.addAttribute("activeTab", "groups");
        return TEMPLATE_GROUP_LIST;
    }

    @GetMapping("/group/create")
    public String showCreateGroupForm(Model model) {
        model.addAttribute(GROUP_NAME, "");
        model.addAttribute("activeTab", "groups");
        return TEMPLATE_GROUP_FORM;
    }

    @PostMapping("/group/create")
    public String createGroup(
            @RequestParam String groupName,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (groupName == null || groupName.trim().isEmpty()) {
            model.addAttribute(ATTR_ERROR, "Group name cannot be empty");
            model.addAttribute(GROUP_NAME, groupName);
            model.addAttribute("activeTab", "groups");
            return TEMPLATE_GROUP_FORM;
        }

        try {
            managerService.createGroup(userDetails.getId(), groupName);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Group created successfully.");
            return REDIRECT_GROUP_LIST;
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTR_ERROR, e.getMessage());
            model.addAttribute(GROUP_NAME, groupName);
            model.addAttribute("activeTab", "groups");
            return TEMPLATE_GROUP_FORM;
        }
    }

    @PostMapping("/group/delete/{id}")
    public String deleteGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            managerService.deleteGroup(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute(
                    ATTR_SUCCESS, "Group permanently deleted. All assigned employees moved to DEFAULT group.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }
        return REDIRECT_GROUP_LIST;
    }

    @GetMapping("/employee/history/{id}")
    public String viewEmployeeHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        int m = month != null ? month : LocalDate.now(ZoneId.of("UTC")).getMonthValue();
        int y = year != null ? year : LocalDate.now(ZoneId.of("UTC")).getYear();

        List<Attendance> history = managerService.getAttendanceHistory(id, m, y);
        model.addAttribute("history", history);
        model.addAttribute("currentMonth", m);
        model.addAttribute("currentYear", y);
        model.addAttribute(ATTR_EMPLOYEE_ID, id);
        model.addAttribute("activeTab", "dashboard");
        return TEMPLATE_ATTENDANCE_HISTORY;
    }
}
