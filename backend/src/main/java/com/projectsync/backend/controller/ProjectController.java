package com.projectsync.backend.controller;

import com.projectsync.backend.dto.CreateProjectRequest;
import com.projectsync.backend.dto.ProjectProgressResponse;
import com.projectsync.backend.dto.ProjectResponse;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<?> createProject(
            Authentication authentication,
            @Valid @RequestBody CreateProjectRequest request) {
        try {
            ProjectResponse response = projectService.createProject(authentication, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/my-project")
    public ResponseEntity<?> getMyProject(Authentication authentication) {
        try {
            ProjectResponse response = projectService.getMyProject(authentication);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/my-project/progress")
    public ResponseEntity<?> getMyProjectProgress(Authentication authentication) {
        try {
            ProjectProgressResponse response = projectService.getMyProjectProgress(authentication);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectById(
            Authentication authentication,
            @PathVariable UUID projectId) {
        try {
            ProjectResponse response = projectService.getProjectById(authentication, projectId);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<?> updateProject(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectRequest request) {
        try {
            ProjectResponse response = projectService.updateProject(authentication, projectId, request);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{projectId}/status")
    public ResponseEntity<?> updateProjectStatus(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestParam String status) {
        try {
            ProjectResponse response = projectService.updateProjectStatus(authentication, projectId, status);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
