package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    Optional<Team> findByCreatedBy(User user);
}