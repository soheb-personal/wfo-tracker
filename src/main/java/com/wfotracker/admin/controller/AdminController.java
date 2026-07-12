package com.wfotracker.admin.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import com.wfotracker.admin.dto.EmployeeDto;
import com.wfotracker.admin.dto.TeamDto;
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
    private static final String ACTIVE = "Active";
    private static final String INACTIVE = "Inactive";
    private static final String NOT_AVAILABLE = "N/A";

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute(ATTR_TEAMS, adminService.getAllTeams());
        return "admin-dashboard";
    }

    @GetMapping("/team/create")
    public String showCreateTeamForm(Model model) {
        model.addAttribute(ATTR_CREATE_TEAM_REQUEST, new CreateTeamRequest("", "", ""));
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

    @GetMapping("/export/xlsx")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=teams_compliance_report.xlsx");

        List<TeamDto> teams = adminService.getAllTeams();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Teams Report");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] columns = {
                "Team Name",
                "Team Active",
                "Manager Name",
                "Manager DAS ID",
                "Employee Name",
                "Employee DAS ID",
                "Employee Active"
            };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            writeTeamRows(sheet, teams);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export/csv")
    public void exportToCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=teams_compliance_report.csv");

        List<TeamDto> teams = adminService.getAllTeams();

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');
            writer.println(
                    "Team Name,Team Active,Manager Name,Manager DAS ID,Employee Name,Employee DAS ID,Employee Active");
            writeCsvRows(writer, teams);
        }
    }

    private void writeTeamRows(Sheet sheet, List<TeamDto> teams) {
        int rowIdx = 1;
        for (TeamDto team : teams) {
            rowIdx = writeTeamDataRows(sheet, team, rowIdx);
        }
    }

    private int writeTeamDataRows(Sheet sheet, TeamDto team, int startRowIdx) {
        int rowIdx = startRowIdx;
        if (team.employees().isEmpty()) {
            Row row = sheet.createRow(rowIdx++);
            populateRowWithPlaceholder(row, team);
        } else {
            for (EmployeeDto emp : team.employees()) {
                Row row = sheet.createRow(rowIdx++);
                populateRowWithEmployee(row, team, emp);
            }
        }
        return rowIdx;
    }

    private void populateRowWithPlaceholder(Row row, TeamDto team) {
        row.createCell(0).setCellValue(team.teamName());
        row.createCell(1).setCellValue(team.active() ? ACTIVE : INACTIVE);
        row.createCell(2).setCellValue(team.managerName() != null ? team.managerName() : NOT_AVAILABLE);
        row.createCell(3).setCellValue(team.managerUsername() != null ? team.managerUsername() : NOT_AVAILABLE);
        row.createCell(4).setCellValue("No Employees");
        row.createCell(5).setCellValue(NOT_AVAILABLE);
        row.createCell(6).setCellValue(NOT_AVAILABLE);
    }

    private void populateRowWithEmployee(Row row, TeamDto team, EmployeeDto emp) {
        row.createCell(0).setCellValue(team.teamName());
        row.createCell(1).setCellValue(team.active() ? ACTIVE : INACTIVE);
        row.createCell(2).setCellValue(team.managerName() != null ? team.managerName() : NOT_AVAILABLE);
        row.createCell(3).setCellValue(team.managerUsername() != null ? team.managerUsername() : NOT_AVAILABLE);
        row.createCell(4).setCellValue(emp.fullName());
        row.createCell(5).setCellValue(emp.username());
        row.createCell(6).setCellValue(emp.active() ? ACTIVE : INACTIVE);
    }

    private void writeCsvRows(PrintWriter writer, List<TeamDto> teams) {
        for (TeamDto team : teams) {
            writeTeamCsvRows(writer, team);
        }
    }

    private void writeTeamCsvRows(PrintWriter writer, TeamDto team) {
        if (team.employees().isEmpty()) {
            writer.println(formatCsvRow(team, null));
        } else {
            for (EmployeeDto emp : team.employees()) {
                writer.println(formatCsvRow(team, emp));
            }
        }
    }

    private String formatCsvRow(TeamDto team, EmployeeDto emp) {
        String teamName = team.teamName();
        String activeStr = team.active() ? ACTIVE : INACTIVE;
        String resolvedMgrName = team.managerName() != null ? team.managerName() : NOT_AVAILABLE;
        String resolvedMgrDasId = team.managerUsername() != null ? team.managerUsername() : NOT_AVAILABLE;

        String empName = "No Employees";
        String empDasId = NOT_AVAILABLE;
        String resolvedEmpActive = NOT_AVAILABLE;

        if (emp != null) {
            empName = emp.fullName();
            empDasId = emp.username();
            resolvedEmpActive = emp.active() ? ACTIVE : INACTIVE;
        }

        return String.format(
                "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                escapeCsv(teamName),
                activeStr,
                escapeCsv(resolvedMgrName),
                escapeCsv(resolvedMgrDasId),
                escapeCsv(empName),
                escapeCsv(empDasId),
                resolvedEmpActive);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }
}
