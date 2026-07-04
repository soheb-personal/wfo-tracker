package com.wfotracker.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wfotracker.domain.entity.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByActiveTrue();

    boolean existsByTeamName(String teamName);
}
