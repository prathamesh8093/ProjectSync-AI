package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByProjectOrderByStartDateAsc(Project project);

    Optional<Sprint> findByIdAndProject(UUID id, Project project);

    boolean existsByProjectAndStatus(Project project, String status);

    boolean existsByProjectAndStatusAndIdNot(
            Project project,
            String status,
            UUID sprintId);
}