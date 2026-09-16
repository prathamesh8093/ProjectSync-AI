package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.TaskComment;
import com.projectsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskCommentRepository
        extends JpaRepository<TaskComment, UUID> {

    List<TaskComment> findByTaskOrderByCreatedAtAsc(
            Task task);

    Optional<TaskComment> findByIdAndTask(
            UUID id,
            Task task);

    List<TaskComment> findByUserOrderByCreatedAtDesc(
            User user);
}