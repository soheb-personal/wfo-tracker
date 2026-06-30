package com.wfotracker.domain.repository;

import com.wfotracker.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findAllByActiveTrue();
    boolean existsByTeamName(String teamName);
}
