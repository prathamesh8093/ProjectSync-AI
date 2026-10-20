package com.projectsync.backend.controller;

import com.projectsync.backend.dto.StudentAssessmentRequest;
import com.projectsync.backend.entity.StudentAssessment;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.StudentAssessmentRepository;
import com.projectsync.backend.repository.UserRepository;
import com.projectsync.backend.service.AiRoleRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AssessmentController {

    private final StudentAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final AiRoleRecommendationService roleRecommendationService;
    private final com.projectsync.backend.service.AiAssessmentService aiAssessmentService;

    public AssessmentController(StudentAssessmentRepository assessmentRepository,
                                UserRepository userRepository,
                                AiRoleRecommendationService roleRecommendationService,
                                com.projectsync.backend.service.AiAssessmentService aiAssessmentService) {
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
        this.roleRecommendationService = roleRecommendationService;
        this.aiAssessmentService = aiAssessmentService;
    }

    @GetMapping("/students/assessment/quiz")
    public ResponseEntity<?> generateQuiz(Authentication authentication) {
        User student = resolveCurrentUser(authentication);
        if (!"STUDENT".equals(student.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can take an assessment quiz.");
        }
        return ResponseEntity.ok(aiAssessmentService.generateQuiz(student));
    }

    @PostMapping("/students/assessment/quiz/submit")
    public ResponseEntity<?> submitQuiz(Authentication authentication,
                                        @RequestBody com.projectsync.backend.dto.QuizSubmitRequest request) {
        User student = resolveCurrentUser(authentication);
        if (!"STUDENT".equals(student.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can submit an assessment quiz.");
        }
        return ResponseEntity.ok(aiAssessmentService.submitQuiz(student, request));
    }

    @PostMapping("/students/assessment")
    public ResponseEntity<?> saveAssessment(Authentication authentication,
                                            @Valid @RequestBody StudentAssessmentRequest request) {
        User student = resolveCurrentUser(authentication);
        if (!"STUDENT".equals(student.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can submit an assessment.");
        }

        StudentAssessment a = assessmentRepository.findById(student.getId()).orElseGet(StudentAssessment::new);
        a.setUserId(student.getId());
        a.setFrontendScore(request.frontendScore());
        a.setBackendScore(request.backendScore());
        a.setDatabaseScore(request.databaseScore());
        a.setDevopsScore(request.devopsScore());
        a.setMlAiScore(request.mlAiScore());
        a.setPrimaryInterest(request.primaryInterest());
        a.setPastProjects(request.pastProjects());
        a.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(assessmentRepository.save(a));
    }

    @GetMapping("/students/assessment/me")
    public ResponseEntity<?> getMyAssessment(Authentication authentication) {
        User student = resolveCurrentUser(authentication);
        return assessmentRepository.findById(student.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("No assessment submitted yet."));
    }

    @PostMapping("/teams/{teamId}/recommend-roles")
    public ResponseEntity<?> recommendRoles(Authentication authentication, @PathVariable UUID teamId) {
        User currentUser = resolveCurrentUser(authentication);
        try {
            return ResponseEntity.ok(roleRecommendationService.recommendRoles(currentUser, teamId));
        } catch (ForbiddenOperationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException | RestClientException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("AI service is unavailable right now.");
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }
}
