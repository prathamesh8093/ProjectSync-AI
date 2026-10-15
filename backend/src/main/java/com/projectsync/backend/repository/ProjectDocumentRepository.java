package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDocumentRepository
        extends JpaRepository<ProjectDocument, UUID> {

    List<ProjectDocument> findByProjectOrderByUpdatedAtDesc(
            Project project);

    Optional<ProjectDocument> findByIdAndProject(
            UUID id,
            Project project);
}