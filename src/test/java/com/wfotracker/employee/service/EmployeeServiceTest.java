package com.wfotracker.employee.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wfotracker.domain.entity.Attendance;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.AttendanceRepository;
import com.wfotracker.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private User employee;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setFullName("Jane Employee");
        employee.setUsername("jane");

        attendance = new Attendance();
        attendance.setId(10L);
        attendance.setEmployee(employee);
        attendance.setOfficeDate(LocalDate.now());
        attendance.setCheckIn(LocalDateTime.now().minusHours(8));
    }

    @Test
    void testCheckIn_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        employeeService.checkIn(1L);

        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testCheckIn_EmployeeNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> employeeService.checkIn(1L));
    }

    @Test
    void testCheckIn_AlreadyCheckedIn() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));

        assertThrows(IllegalStateException.class, () -> employeeService.checkIn(1L));
    }

    @Test
    void testCheckOut_Success() {
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));

        employeeService.checkOut(1L);

        assertNotNull(attendance.getCheckOut());
        assertTrue(attendance.getHoursSpent().compareTo(BigDecimal.ZERO) > 0);
        verify(attendanceRepository).save(attendance);
    }

    private void assertNotNull(Object obj) {
        assertTrue(obj != null);
    }

    @Test
    void testCheckOut_NoCheckInFound() {
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> employeeService.checkOut(1L));
    }

    @Test
    void testCheckOut_AlreadyCheckedOut() {
        attendance.setCheckOut(LocalDateTime.now());
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));

        assertThrows(IllegalStateException.class, () -> employeeService.checkOut(1L));
    }

    @Test
    void testGetAttendanceHistory() {
        when(attendanceRepository.findByEmployeeIdAndMonthAndYear(1L, 7, 2026)).thenReturn(List.of(attendance));

        List<Attendance> history = employeeService.getAttendanceHistory(1L, 7, 2026);
        assertEquals(1, history.size());
    }

    @Test
    void testIsCheckedInToday_True() {
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        assertTrue(employeeService.isCheckedInToday(1L));
    }

    @Test
    void testIsCheckedInToday_False() {
        attendance.setCheckOut(LocalDateTime.now());
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        assertFalse(employeeService.isCheckedInToday(1L));
    }

    @Test
    void testIsCheckedOutToday_True() {
        attendance.setCheckOut(LocalDateTime.now());
        when(attendanceRepository.findByEmployeeIdAndOfficeDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        assertTrue(employeeService.isCheckedOutToday(1L));
    }
}
