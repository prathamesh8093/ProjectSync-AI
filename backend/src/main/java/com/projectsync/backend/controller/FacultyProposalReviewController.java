package com.projectsync.backend.controller;

import com.projectsync.backend.dto.FacultyProposalResponse;
import com.projectsync.backend.dto.ReviewProposalRequest;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.FacultyProposalReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/faculty/proposals")
public class FacultyProposalReviewController {

    private final FacultyProposalReviewService facultyProposalReviewService;

    public FacultyProposalReviewController(FacultyProposalReviewService facultyProposalReviewService) {
        this.facultyProposalReviewService = facultyProposalReviewService;
    }

    @GetMapping
    public ResponseEntity<?> listSubmittedProposals(Authentication authentication) {
        try {
            List<FacultyProposalResponse> response =
                    facultyProposalReviewService.listSubmittedProposals(authentication);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/{proposalId}")
    public ResponseEntity<?> getProposalDetail(Authentication authentication,
                                                @PathVariable UUID proposalId) {
        try {
            FacultyProposalResponse response =
                    facultyProposalReviewService.getProposalDetail(authentication, proposalId);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{proposalId}/review")
    public ResponseEntity<?> reviewProposal(Authentication authentication,
                                             @PathVariable UUID proposalId,
                                             @Valid @RequestBody ReviewProposalRequest request) {
        try {
            FacultyProposalResponse response =
                    facultyProposalReviewService.reviewProposal(authentication, proposalId, request);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}