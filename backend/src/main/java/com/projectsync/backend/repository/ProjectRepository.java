package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByProposal(ProjectProposal proposal);

    Optional<Project> findByTeam(Team team);

    boolean existsByProposal(ProjectProposal proposal);
}