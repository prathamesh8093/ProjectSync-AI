package com.projectsync.backend.controller;

import com.projectsync.backend.dto.CreateSprintRequest;
import com.projectsync.backend.dto.SprintResponse;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @PostMapping
    public ResponseEntity<?> createSprint(
            Authentication authentication,
            @Valid @RequestBody CreateSprintRequest request) {

        try {
            SprintResponse response =
                    sprintService.createSprint(
                            authentication,
                            request);

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
    public ResponseEntity<?> getMyProjectSprints(
            Authentication authentication) {

        try {
            List<SprintResponse> response =
                    sprintService.getMyProjectSprints(
                            authentication);

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

    @GetMapping("/{sprintId}")
    public ResponseEntity<?> getSprintById(
            Authentication authentication,
            @PathVariable UUID sprintId) {

        try {
            SprintResponse response =
                    sprintService.getSprintById(
                            authentication,
                            sprintId);

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

    @PutMapping("/{sprintId}")
    public ResponseEntity<?> updateSprint(
            Authentication authentication,
            @PathVariable UUID sprintId,
            @Valid @RequestBody CreateSprintRequest request) {

        try {
            SprintResponse response =
                    sprintService.updateSprint(
                            authentication,
                            sprintId,
                            request);

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

    @PutMapping("/{sprintId}/status")
    public ResponseEntity<?> updateSprintStatus(
            Authentication authentication,
            @PathVariable UUID sprintId,
            @RequestParam String status) {

        try {
            SprintResponse response =
                    sprintService.updateSprintStatus(
                            authentication,
                            sprintId,
                            status);

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