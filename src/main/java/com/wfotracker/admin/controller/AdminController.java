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

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("teams", adminService.getAllTeams());
        return "admin-dashboard";
    }

    @GetMapping("/team/create")
    public String showCreateTeamForm(Model model) {
        model.addAttribute("createTeamRequest", new CreateTeamRequest("", ""));
        return "team-form";
    }

    @PostMapping("/team/create")
    public String createTeam(
            @Valid @ModelAttribute("createTeamRequest") CreateTeamRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "team-form";
        }

        try {
            adminService.createTeam(request);
            redirectAttributes.addFlashAttribute("success", "Team and Manager created successfully.");
            return "redirect:/admin/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "team-form";
        }
    }

    @GetMapping("/team/list")
    public String listTeams(Model model) {
        model.addAttribute("teams", adminService.getAllTeams());
        return "team-list";
    }

    @GetMapping("/team/edit/{id}")
    public String showEditTeamForm(@PathVariable Long id, Model model) {
        model.addAttribute("editTeamRequest", adminService.getTeamForEdit(id));
        model.addAttribute("teamId", id);
        return "team-edit";
    }

    @PostMapping("/team/edit/{id}")
    public String editTeam(
            @PathVariable Long id,
            @Valid @ModelAttribute("editTeamRequest") EditTeamRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("teamId", id);
            return "team-edit";
        }

        try {
            adminService.editTeam(id, request);
            redirectAttributes.addFlashAttribute("success", "Team updated successfully.");
            return "redirect:/admin/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("teamId", id);
            return "team-edit";
        }
    }

    @PostMapping("/team/deactivate/{id}")
    public String deactivateTeam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deactivateTeam(id);
            redirectAttributes.addFlashAttribute("success", "Team deactivated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/manager/reset-password/{id}")
    public String resetManagerPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.resetManagerPassword(id);
            redirectAttributes.addFlashAttribute("success", "Manager password reset successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
