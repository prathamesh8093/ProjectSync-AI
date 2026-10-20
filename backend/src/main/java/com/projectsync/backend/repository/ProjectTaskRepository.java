package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {
    List<ProjectTask> findBySprint(Sprint sprint);
    List<ProjectTask> findBySprint_Project(Project project);
}
