package com.wfotracker.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wfotracker.domain.entity.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndOfficeDate(Long employeeId, LocalDate officeDate);

    List<Attendance> findByEmployeeId(Long employeeId);

    @Query(
            "SELECT a FROM Attendance a WHERE a.employee.id = :employeeId AND YEAR(a.officeDate) = :year AND MONTH(a.officeDate) = :month ORDER BY a.officeDate DESC")
    List<Attendance> findByEmployeeIdAndMonthAndYear(
            @Param("employeeId") Long employeeId, @Param("month") int month, @Param("year") int year);

    @Query(
            "SELECT COUNT(a) FROM Attendance a WHERE a.employee.id = :employeeId AND YEAR(a.officeDate) = :year AND MONTH(a.officeDate) = :month AND ((a.attendanceType = 'NORMAL' AND a.checkIn IS NOT NULL) OR a.attendanceType = 'MANUAL_ENTRY')")
    int countVisitedDaysByEmployeeIdAndMonthAndYear(
            @Param("employeeId") Long employeeId, @Param("month") int month, @Param("year") int year);
}
