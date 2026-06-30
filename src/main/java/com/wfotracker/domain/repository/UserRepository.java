package com.wfotracker.domain.repository;

import com.wfotracker.common.constants.Role;
import com.wfotracker.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndActiveTrue(String username);
    Optional<User> findByUsername(String username);
    List<User> findByManagerIdAndRoleAndActiveTrue(Long managerId, Role role);
    List<User> findByTeamIdAndRole(Long teamId, Role role);
    List<User> findByTeamId(Long teamId);
    boolean existsByUsername(String username);
}
