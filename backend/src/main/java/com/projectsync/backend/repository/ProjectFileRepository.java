package com.projectsync.backend.repository;

import com.projectsync.backend.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, UUID> {

    List<ProjectFile> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ProjectFile> findByIdAndProjectId(
            UUID id,
            UUID projectId
    );
}