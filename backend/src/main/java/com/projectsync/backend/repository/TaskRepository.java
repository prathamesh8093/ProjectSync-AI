package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProjectOrderByCreatedAtAsc(Project project);

    List<Task> findByProjectAndSprintOrderByCreatedAtAsc(
            Project project,
            Sprint sprint);

    List<Task> findByProjectAndAssignedToOrderByCreatedAtAsc(
            Project project,
            User assignedTo);

    Optional<Task> findByIdAndProject(
            UUID id,
            Project project);
}