package com.projectsync.backend.controller;

import com.projectsync.backend.dto.TaskHistoryResponse;
import com.projectsync.backend.service.TaskHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/task-history")
public class TaskHistoryController {

    private final TaskHistoryService taskHistoryService;

    public TaskHistoryController(
            TaskHistoryService taskHistoryService) {

        this.taskHistoryService = taskHistoryService;
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskHistoryResponse>> getTaskHistory(
            @PathVariable UUID taskId,
            Authentication authentication) {

        List<TaskHistoryResponse> response =
                taskHistoryService.getTaskHistory(
                        taskId,
                        authentication.getName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{historyId}")
    public ResponseEntity<TaskHistoryResponse> getHistoryById(
            @PathVariable UUID historyId,
            Authentication authentication) {

        TaskHistoryResponse response =
                taskHistoryService.getHistoryById(
                        historyId,
                        authentication.getName());

        return ResponseEntity.ok(response);
    }
}