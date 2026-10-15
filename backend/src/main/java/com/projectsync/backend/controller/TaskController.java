package com.projectsync.backend.controller;

import com.projectsync.backend.dto.CreateTaskRequest;
import com.projectsync.backend.dto.TaskResponse;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<?> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {

        try {
            TaskResponse response =
                    taskService.createTask(
                            request,
                            authentication.getName());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/my-project")
    public ResponseEntity<?> getMyProjectTasks(
            Authentication authentication) {

        try {
            List<TaskResponse> response =
                    taskService.getMyProjectTasks(
                            authentication.getName());

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<?> getTasksBySprint(
            @PathVariable UUID sprintId,
            Authentication authentication) {

        try {
            List<TaskResponse> response =
                    taskService.getTasksBySprint(
                            sprintId,
                            authentication.getName());

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(
            @PathVariable UUID taskId,
            Authentication authentication) {

        try {
            TaskResponse response =
                    taskService.getTaskById(
                            taskId,
                            authentication.getName());

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {

        try {
            TaskResponse response =
                    taskService.updateTask(
                            taskId,
                            request,
                            authentication.getName());

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable UUID taskId,
            @RequestParam String status,
            Authentication authentication) {

        try {
            TaskResponse response =
                    taskService.updateTaskStatus(
                            taskId,
                            status,
                            authentication.getName());

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{taskId}/progress")
    public ResponseEntity<?> updateTaskProgress(
            @PathVariable UUID taskId,
            @RequestParam Integer progress,
            Authentication authentication) {

        try {
            TaskResponse response =
                    taskService.updateTaskProgress(
                            taskId,
                            progress,
                            authentication.getName());

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (ConflictException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }
}