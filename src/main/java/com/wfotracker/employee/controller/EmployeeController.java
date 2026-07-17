package com.wfotracker.employee.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import com.wfotracker.domain.entity.Attendance;
import com.wfotracker.employee.service.EmployeeService;
import com.wfotracker.manager.dto.EmployeeComplianceDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private static final String HISTORY = "history";

    private final EmployeeService employeeService;
    private final ComplianceService complianceService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        LocalDate now = LocalDate.now(ZoneId.of("UTC"));
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        EmployeeComplianceDto compliance =
                complianceService.getComplianceForEmployee(userDetails.getUser(), currentMonth, currentYear);

        model.addAttribute("compliance", compliance);
        model.addAttribute("isCheckedIn", employeeService.isCheckedInToday(userDetails.getId()));
        model.addAttribute("isCheckedOut", employeeService.isCheckedOutToday(userDetails.getId()));
        model.addAttribute(
                HISTORY, employeeService.getAttendanceHistory(userDetails.getId(), currentMonth, currentYear));
        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("today", now);
        model.addAttribute("activeTab", "dashboard");

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
        int m = month != null ? month : LocalDate.now(ZoneId.of("UTC")).getMonthValue();
        int y = year != null ? year : LocalDate.now(ZoneId.of("UTC")).getYear();

        model.addAttribute(HISTORY, employeeService.getAttendanceHistory(userDetails.getId(), m, y));
        model.addAttribute("currentMonth", m);
        model.addAttribute("currentYear", y);
        model.addAttribute("today", LocalDate.now(ZoneId.of("UTC")));
        model.addAttribute("activeTab", HISTORY);
        return "attendance-history";
    }

    @GetMapping("/export/xlsx")
    public void exportToExcel(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response)
            throws IOException {
        int m = month != null ? month : LocalDate.now(ZoneId.of("UTC")).getMonthValue();
        int y = year != null ? year : LocalDate.now(ZoneId.of("UTC")).getYear();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition", String.format("attachment; filename=attendance_history_%d_%d.xlsx", m, y));

        List<Attendance> history = employeeService.getAttendanceHistory(userDetails.getId(), m, y);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance History");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date", "Check-In", "Check-Out", "Hours Spent", "Type"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            int rowIdx = 1;
            for (Attendance att : history) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(att.getOfficeDate().toString());
                row.createCell(1)
                        .setCellValue(
                                att.getCheckInLocal() != null
                                        ? att.getCheckInLocal().toLocalTime().toString()
                                        : "-");

                String checkoutVal = "-";
                String hoursVal = "-";
                if (att.getCheckOutLocal() != null) {
                    checkoutVal = att.getCheckOutLocal().toLocalTime().toString();
                    hoursVal = att.getHoursSpent() != null ? att.getHoursSpent().toString() + " hrs" : "-";
                } else if (att.getOfficeDate().isBefore(today)) {
                    checkoutVal = "Forgot";
                }

                row.createCell(2).setCellValue(checkoutVal);
                row.createCell(3).setCellValue(hoursVal);
                row.createCell(4).setCellValue(att.getAttendanceType());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export/csv")
    public void exportToCsv(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response)
            throws IOException {
        int m = month != null ? month : LocalDate.now(ZoneId.of("UTC")).getMonthValue();
        int y = year != null ? year : LocalDate.now(ZoneId.of("UTC")).getYear();

        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition", String.format("attachment; filename=attendance_history_%d_%d.csv", m, y));

        List<Attendance> history = employeeService.getAttendanceHistory(userDetails.getId(), m, y);

        try (java.io.PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff'); // BOM for Excel UTF-8 compliance
            writer.println("Date,Check-In,Check-Out,Hours Spent,Type");

            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            for (Attendance att : history) {
                String checkoutVal = "-";
                String hoursVal = "-";
                if (att.getCheckOutLocal() != null) {
                    checkoutVal = att.getCheckOutLocal().toLocalTime().toString();
                    hoursVal = att.getHoursSpent() != null ? att.getHoursSpent().toString() + " hrs" : "-";
                } else if (att.getOfficeDate().isBefore(today)) {
                    checkoutVal = "Forgot";
                }

                writer.println(String.format(
                        "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        att.getOfficeDate().toString(),
                        att.getCheckInLocal() != null
                                ? att.getCheckInLocal().toLocalTime().toString()
                                : "-",
                        checkoutVal,
                        hoursVal,
                        att.getAttendanceType()));
            }
        }
    }
}
