package com.wfotracker.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndActiveTrue(String username);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query(
            "SELECT em.employee FROM EmployeeMembership em JOIN em.employee.roles r WHERE em.manager.id = :managerId AND r.name = 'ROLE_EMPLOYEE' AND em.active = true")
    List<User> findEmployeesByManagerIdAndActiveTrue(@Param("managerId") Long managerId);

    @Query(
            "SELECT em.employee FROM EmployeeMembership em JOIN em.employee.roles r WHERE em.team.id = :teamId AND r.name = 'ROLE_EMPLOYEE' AND em.active = true")
    List<User> findEmployeesByTeamIdAndActiveTrue(@Param("teamId") Long teamId);

    @Query("SELECT tm.manager FROM TeamManager tm WHERE tm.team.id = :teamId AND tm.active = true")
    List<User> findManagersByTeamIdAndActiveTrue(@Param("teamId") Long teamId);

    @Query("SELECT DISTINCT em.employee FROM EmployeeMembership em WHERE em.team.id = :teamId")
    List<User> findByTeamId(@Param("teamId") Long teamId);

    default List<User> findByManagerIdAndRoleAndActiveTrue(Long managerId, Role role) {
        if (role == Role.EMPLOYEE) {
            return findEmployeesByManagerIdAndActiveTrue(managerId);
        }
        return List.of();
    }

    default List<User> findByTeamIdAndRole(Long teamId, Role role) {
        if (role == Role.EMPLOYEE) {
            return findEmployeesByTeamIdAndActiveTrue(teamId);
        } else if (role == Role.MANAGER) {
            return findManagersByTeamIdAndActiveTrue(teamId);
        }
        return List.of();
    }
}
