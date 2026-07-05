package com.wfotracker.admin.controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.wfotracker.admin.dto.CreateTeamRequest;
import com.wfotracker.admin.dto.EditTeamRequest;
import com.wfotracker.admin.service.AdminService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final String REDIRECT_ADMIN_DASHBOARD = "redirect:/admin/dashboard";
    private static final String TEMPLATE_TEAM_FORM = "team-form";
    private static final String TEMPLATE_TEAM_EDIT = "team-edit";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_TEAM_ID = "teamId";
    private static final String ATTR_TEAMS = "teams";
    private static final String ATTR_CREATE_TEAM_REQUEST = "createTeamRequest";
    private static final String ATTR_EDIT_TEAM_REQUEST = "editTeamRequest";

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute(ATTR_TEAMS, adminService.getAllTeams());
        return "admin-dashboard";
    }

    @GetMapping("/team/create")
    public String showCreateTeamForm(Model model) {
        model.addAttribute(ATTR_CREATE_TEAM_REQUEST, new CreateTeamRequest("", ""));
        return TEMPLATE_TEAM_FORM;
    }

    @PostMapping("/team/create")
    public String createTeam(
            @Valid @ModelAttribute(ATTR_CREATE_TEAM_REQUEST) CreateTeamRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            return TEMPLATE_TEAM_FORM;
        }

        try {
            adminService.createTeam(request);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Team and Manager created successfully.");
            return REDIRECT_ADMIN_DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTR_ERROR, e.getMessage());
            return TEMPLATE_TEAM_FORM;
        }
    }

    @GetMapping("/team/list")
    public String listTeams(Model model) {
        model.addAttribute(ATTR_TEAMS, adminService.getAllTeams());
        return "team-list";
    }

    @GetMapping("/team/edit/{id}")
    public String showEditTeamForm(@PathVariable Long id, Model model) {
        model.addAttribute(ATTR_EDIT_TEAM_REQUEST, adminService.getTeamForEdit(id));
        model.addAttribute(ATTR_TEAM_ID, id);
        return TEMPLATE_TEAM_EDIT;
    }

    @PostMapping("/team/edit/{id}")
    public String editTeam(
            @PathVariable Long id,
            @Valid @ModelAttribute(ATTR_EDIT_TEAM_REQUEST) EditTeamRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_TEAM_ID, id);
            return TEMPLATE_TEAM_EDIT;
        }

        try {
            adminService.editTeam(id, request);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Team updated successfully.");
            return REDIRECT_ADMIN_DASHBOARD;
        } catch (IllegalArgumentException e) {
            model.addAttribute(ATTR_ERROR, e.getMessage());
            model.addAttribute(ATTR_TEAM_ID, id);
            return TEMPLATE_TEAM_EDIT;
        }
    }

    @PostMapping("/team/deactivate/{id}")
    public String deactivateTeam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deactivateTeam(id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Team deactivated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }
        return REDIRECT_ADMIN_DASHBOARD;
    }

    @PostMapping("/manager/reset-password/{id}")
    public String resetManagerPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.resetManagerPassword(id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS, "Manager password reset successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }
        return REDIRECT_ADMIN_DASHBOARD;
    }
}
