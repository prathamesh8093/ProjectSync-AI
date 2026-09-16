package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.TaskHistory;
import com.projectsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskHistoryRepository
        extends JpaRepository<TaskHistory, UUID> {

    List<TaskHistory> findByTaskOrderByCreatedAtDesc(
            Task task);

    List<TaskHistory> findByUserOrderByCreatedAtDesc(
            User user);
}