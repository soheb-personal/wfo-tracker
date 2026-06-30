package com.wfotracker.domain.repository;

import com.wfotracker.domain.entity.MonthlyConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyConfigurationRepository extends JpaRepository<MonthlyConfiguration, Long> {
    Optional<MonthlyConfiguration> findByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);
}
