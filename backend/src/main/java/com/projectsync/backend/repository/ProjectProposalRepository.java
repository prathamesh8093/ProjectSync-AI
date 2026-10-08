package com.projectsync.backend.repository;

import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectProposalRepository extends JpaRepository<ProjectProposal, UUID> {
    Optional<ProjectProposal> findByTeam(Team team);
    boolean existsByTeam(Team team);
    List<ProjectProposal> findByStatus(String status);
    Optional<ProjectProposal> findByProjectCode(String projectCode);
    Optional<ProjectProposal> findByProjectCodeIgnoreCase(String projectCode);
    boolean existsByProjectCode(String projectCode);
}