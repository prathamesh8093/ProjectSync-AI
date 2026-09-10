package com.projectsync.backend.controller;

import com.projectsync.backend.dto.AddTeamMemberRequest;
import com.projectsync.backend.dto.CreateTeamRequest;
import com.projectsync.backend.dto.TeamMemberResponse;
import com.projectsync.backend.dto.TeamResponse;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<?> createTeam(Authentication authentication,
                                         @Valid @RequestBody CreateTeamRequest request) {
        try {
            TeamResponse response = teamService.createTeam(authentication, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/my-team")
    public ResponseEntity<?> getMyTeam(Authentication authentication) {
        try {
            TeamResponse response = teamService.getMyTeam(authentication);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<?> addMember(Authentication authentication,
                                        @PathVariable UUID teamId,
                                        @Valid @RequestBody AddTeamMemberRequest request) {
        try {
            TeamMemberResponse response = teamService.addMember(authentication, teamId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> getTeamMembers(Authentication authentication,
                                             @PathVariable UUID teamId) {
        try {
            List<TeamMemberResponse> response = teamService.getTeamMembers(authentication, teamId);
            return ResponseEntity.ok(response);
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}