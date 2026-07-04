package com.wfotracker.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wfotracker.domain.entity.MonthlyConfiguration;

@Repository
public interface MonthlyConfigurationRepository extends JpaRepository<MonthlyConfiguration, Long> {
    Optional<MonthlyConfiguration> findByEmployeeIdAndMonthAndYear(Long employeeId, int month, int year);
}
