package com.projectsync.backend.controller;

import com.projectsync.backend.dto.CreateTaskCommentRequest;
import com.projectsync.backend.dto.TaskCommentResponse;
import com.projectsync.backend.service.TaskCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/task-comments")
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    public TaskCommentController(
            TaskCommentService taskCommentService) {

        this.taskCommentService = taskCommentService;
    }

    @PostMapping
    public ResponseEntity<TaskCommentResponse> createComment(
            @Valid @RequestBody CreateTaskCommentRequest request,
            Authentication authentication) {

        TaskCommentResponse response =
                taskCommentService.createComment(
                        request,
                        authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskCommentResponse>> getTaskComments(
            @PathVariable UUID taskId,
            Authentication authentication) {

        List<TaskCommentResponse> response =
                taskCommentService.getTaskComments(
                        taskId,
                        authentication.getName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<TaskCommentResponse> getCommentById(
            @PathVariable UUID commentId,
            Authentication authentication) {

        TaskCommentResponse response =
                taskCommentService.getCommentById(
                        commentId,
                        authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<TaskCommentResponse> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateTaskCommentRequest request,
            Authentication authentication) {

        TaskCommentResponse response =
                taskCommentService.updateComment(
                        commentId,
                        request,
                        authentication.getName());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication) {

        taskCommentService.deleteComment(
                commentId,
                authentication.getName());

        return ResponseEntity.noContent().build();
    }
}