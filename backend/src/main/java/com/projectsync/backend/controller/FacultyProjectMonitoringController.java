package com.projectsync.backend.controller;

import com.projectsync.backend.dto.FacultyProjectMonitoringResponse;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.FacultyProjectMonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/faculty/projects")
public class FacultyProjectMonitoringController {

    private final FacultyProjectMonitoringService facultyProjectMonitoringService;

    public FacultyProjectMonitoringController(
            FacultyProjectMonitoringService facultyProjectMonitoringService) {

        this.facultyProjectMonitoringService =
                facultyProjectMonitoringService;
    }

    @GetMapping
    public ResponseEntity<?> getApprovedProjects(
            Authentication authentication) {

        try {
            List<FacultyProjectMonitoringResponse> response =
                    facultyProjectMonitoringService.getApprovedProjects(
                            authentication
                    );

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<?> getApprovedProject(
            Authentication authentication,
            @PathVariable UUID projectId) {

        try {
            FacultyProjectMonitoringResponse response =
                    facultyProjectMonitoringService.getApprovedProject(
                            authentication,
                            projectId
                    );

            return ResponseEntity.ok(response);

        } catch (ForbiddenOperationException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());

        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}