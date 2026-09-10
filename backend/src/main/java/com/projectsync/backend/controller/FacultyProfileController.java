package com.projectsync.backend.controller;

import com.projectsync.backend.dto.FacultyProfileRequest;
import com.projectsync.backend.dto.FacultyProfileResponse;
import com.projectsync.backend.entity.FacultyProfile;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.UserRepository;
import com.projectsync.backend.service.FacultyProfileService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faculty-profile")
public class FacultyProfileController {

    private final FacultyProfileService facultyProfileService;
    private final UserRepository userRepository;

    public FacultyProfileController(
            FacultyProfileService facultyProfileService,
            UserRepository userRepository) {

        this.facultyProfileService = facultyProfileService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Object> getMyProfile(
            Authentication authentication) {

        User currentUser = resolveCurrentUser(authentication);

        if (!"FACULTY".equals(currentUser.getRole())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only faculty can access this resource.");
        }

        FacultyProfile profile =
                facultyProfileService
                        .getByUserId(currentUser.getId())
                        .orElse(null);

        if (profile == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Faculty profile has not been created yet.");
        }

        FacultyProfileResponse response =
                facultyProfileService.toResponse(profile);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Object> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody FacultyProfileRequest request) {

        User currentUser = resolveCurrentUser(authentication);

        if (!"FACULTY".equals(currentUser.getRole())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only faculty can update this resource.");
        }

        try {

            FacultyProfile saved =
                    facultyProfileService.upsert(
                            currentUser.getId(),
                            request
                    );

            FacultyProfileResponse response =
                    facultyProfileService.toResponse(saved);

            return ResponseEntity.ok(response);

        } catch (DataIntegrityViolationException e) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("This Faculty ID is already in use.");
        }
    }

    private User resolveCurrentUser(
            Authentication authentication) {

        String email = authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found."
                        )
                );
    }
}