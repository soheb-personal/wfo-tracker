package com.wfotracker.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wfotracker.domain.entity.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByTeamIdAndActiveTrue(Long teamId);

    Optional<Group> findByTeamIdAndGroupNameAndActiveTrue(Long teamId, String groupName);

    boolean existsByTeamIdAndGroupNameAndActiveTrue(Long teamId, String groupName);
}
