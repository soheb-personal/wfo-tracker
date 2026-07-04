package com.wfotracker.employee.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.domain.entity.Attendance;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void checkIn(Long employeeId) {
        User employee = userRepository
                .findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        LocalDate today = LocalDate.now();
        if (attendanceRepository
                .findByEmployeeIdAndOfficeDate(employeeId, today)
                .isPresent()) {
            throw new IllegalStateException("Already checked in for today");
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setOfficeDate(today);
        attendance.setCheckIn(LocalDateTime.now());

        attendanceRepository.save(attendance);
    }

    @Transactional
    public void checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndOfficeDate(employeeId, today)
                .orElseThrow(() -> new IllegalStateException("No check-in found for today"));

        if (attendance.getCheckOut() != null) {
            throw new IllegalStateException("Already checked out for today");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOut(now);

        Duration duration = Duration.between(attendance.getCheckIn(), now);
        BigDecimal hoursSpent =
                BigDecimal.valueOf(duration.toMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        attendance.setHoursSpent(hoursSpent);

        attendanceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceHistory(Long employeeId, int month, int year) {
        return attendanceRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year);
    }

    @Transactional(readOnly = true)
    public boolean isCheckedInToday(Long employeeId) {
        return attendanceRepository
                .findByEmployeeIdAndOfficeDate(employeeId, LocalDate.now())
                .map(a -> a.getCheckIn() != null && a.getCheckOut() == null)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isCheckedOutToday(Long employeeId) {
        return attendanceRepository
                .findByEmployeeIdAndOfficeDate(employeeId, LocalDate.now())
                .map(a -> a.getCheckOut() != null)
                .orElse(false);
    }
}
