package com.wfotracker.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wfotracker.domain.entity.TeamManager;

@Repository
public interface TeamManagerRepository extends JpaRepository<TeamManager, Long> {
    Optional<TeamManager> findByTeamIdAndActiveTrue(Long teamId);

    Optional<TeamManager> findByManagerIdAndActiveTrue(Long managerId);

    boolean existsByManagerIdAndActiveTrue(Long managerId);
}
