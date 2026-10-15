package com.projectsync.backend.controller;

import com.projectsync.backend.dto.CreateProjectDocumentRequest;
import com.projectsync.backend.dto.ProjectDocumentResponse;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.ProjectDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project-documents")
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;

    public ProjectDocumentController(
            ProjectDocumentService projectDocumentService) {

        this.projectDocumentService = projectDocumentService;
    }

    @PostMapping
    public ResponseEntity<?> createDocument(
            @Valid @RequestBody CreateProjectDocumentRequest request,
            Authentication authentication) {

        try {

            ProjectDocumentResponse response =
                    projectDocumentService.createDocument(
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
    public ResponseEntity<?> getMyProjectDocuments(
            Authentication authentication) {

        try {

            List<ProjectDocumentResponse> response =
                    projectDocumentService
                            .getMyProjectDocuments(
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

    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentById(
            @PathVariable UUID documentId,
            Authentication authentication) {

        try {

            ProjectDocumentResponse response =
                    projectDocumentService.getDocumentById(
                            documentId,
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

    @PutMapping("/{documentId}")
    public ResponseEntity<?> updateDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody CreateProjectDocumentRequest request,
            Authentication authentication) {

        try {

            ProjectDocumentResponse response =
                    projectDocumentService.updateDocument(
                            documentId,
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

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable UUID documentId,
            Authentication authentication) {

        try {

            projectDocumentService.deleteDocument(
                    documentId,
                    authentication.getName());

            return ResponseEntity.noContent().build();

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