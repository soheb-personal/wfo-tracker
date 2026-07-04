package com.wfotracker.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wfotracker.domain.entity.EmployeeMembership;

@Repository
public interface EmployeeMembershipRepository extends JpaRepository<EmployeeMembership, Long> {
    Optional<EmployeeMembership> findByEmployeeIdAndActiveTrue(Long employeeId);

    List<EmployeeMembership> findByTeamIdAndActiveTrue(Long teamId);

    List<EmployeeMembership> findByManagerIdAndActiveTrue(Long managerId);

    List<EmployeeMembership> findByGroupIdAndActiveTrue(Long groupId);

    boolean existsByEmployeeIdAndActiveTrue(Long employeeId);
}
