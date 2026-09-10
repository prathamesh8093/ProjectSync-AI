package com.projectsync.backend.controller;

import com.projectsync.backend.dto.StudentProfileRequest;
import com.projectsync.backend.dto.StudentProfileResponse;
import com.projectsync.backend.entity.StudentProfile;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.UserRepository;
import com.projectsync.backend.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student-profile")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final UserRepository userRepository;

    public StudentProfileController(
            StudentProfileService studentProfileService,
            UserRepository userRepository) {

        this.studentProfileService = studentProfileService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Object> getMyProfile(
            Authentication authentication) {

        User currentUser = resolveCurrentUser(authentication);

        if (!"STUDENT".equals(currentUser.getRole())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only students can access this resource.");
        }

        StudentProfile profile =
                studentProfileService
                        .getByUserId(currentUser.getId())
                        .orElse(null);

        if (profile == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Student profile has not been created yet.");
        }

        StudentProfileResponse response =
                studentProfileService.toResponse(profile);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<Object> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody StudentProfileRequest request) {

        User currentUser = resolveCurrentUser(authentication);

        if (!"STUDENT".equals(currentUser.getRole())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Only students can update this resource.");
        }

        try {

            StudentProfile saved =
                    studentProfileService.upsert(
                            currentUser.getId(),
                            request
                    );

            StudentProfileResponse response =
                    studentProfileService.toResponse(saved);

            return ResponseEntity.ok(response);

        } catch (DataIntegrityViolationException e) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("This Student ID is already in use.");
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