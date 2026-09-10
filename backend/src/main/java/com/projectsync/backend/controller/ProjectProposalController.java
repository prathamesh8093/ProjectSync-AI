package com.projectsync.backend.controller;

import com.projectsync.backend.dto.CreateProposalRequest;
import com.projectsync.backend.dto.ProjectProposalResponse;
import com.projectsync.backend.dto.UpdateProposalRequest;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.ProjectProposalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/proposals")
public class ProjectProposalController {

    private final ProjectProposalService proposalService;

    public ProjectProposalController(ProjectProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    public ResponseEntity<?> createProposal(Authentication authentication,
                                             @Valid @RequestBody CreateProposalRequest request) {
        try {
            ProjectProposalResponse response = proposalService.createProposal(authentication, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/my-proposal")
    public ResponseEntity<?> getMyProposal(Authentication authentication) {
        try {
            ProjectProposalResponse response = proposalService.getMyProposal(authentication);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{proposalId}")
    public ResponseEntity<?> updateProposal(Authentication authentication,
                                             @PathVariable UUID proposalId,
                                             @Valid @RequestBody UpdateProposalRequest request) {
        try {
            ProjectProposalResponse response = proposalService.updateProposal(authentication, proposalId, request);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/{proposalId}/submit")
    public ResponseEntity<?> submitProposal(Authentication authentication,
                                             @PathVariable UUID proposalId) {
        try {
            ProjectProposalResponse response = proposalService.submitProposal(authentication, proposalId);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/{proposalId}")
    public ResponseEntity<?> getProposalById(Authentication authentication,
                                              @PathVariable UUID proposalId) {
        try {
            ProjectProposalResponse response = proposalService.getProposalById(authentication, proposalId);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}