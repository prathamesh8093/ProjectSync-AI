package com.projectsync.backend.service;

import com.projectsync.backend.dto.FacultyProposalResponse;
import com.projectsync.backend.dto.ReviewProposalRequest;
import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.ProposalReview;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectProposalRepository;
import com.projectsync.backend.repository.ProposalReviewRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FacultyProposalReviewService {

    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String DECISION_APPROVED = "APPROVED";
    private static final String DECISION_REJECTED = "REJECTED";

    private final ProjectProposalRepository proposalRepository;
    private final ProposalReviewRepository proposalReviewRepository;
    private final UserRepository userRepository;

    public FacultyProposalReviewService(ProjectProposalRepository proposalRepository,
                                         ProposalReviewRepository proposalReviewRepository,
                                         UserRepository userRepository) {
        this.proposalRepository = proposalRepository;
        this.proposalReviewRepository = proposalReviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FacultyProposalResponse> listSubmittedProposals(Authentication authentication) {
        resolveFaculty(authentication);

        return proposalRepository.findByStatus(STATUS_SUBMITTED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FacultyProposalResponse getProposalDetail(Authentication authentication, UUID proposalId) {
        resolveFaculty(authentication);

        ProjectProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found."));

        return toResponse(proposal);
    }

    @Transactional
    public FacultyProposalResponse reviewProposal(Authentication authentication,
                                                    UUID proposalId,
                                                    ReviewProposalRequest request) {
        User faculty = resolveFaculty(authentication);

        ProjectProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found."));

        if (!STATUS_SUBMITTED.equals(proposal.getStatus())) {
            throw new ConflictException("Only proposals with status SUBMITTED can be reviewed.");
        }

        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        if (!DECISION_APPROVED.equals(decision) && !DECISION_REJECTED.equals(decision)) {
            throw new BadRequestException("decision must be APPROVED or REJECTED.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        ProposalReview review = new ProposalReview();
        review.setProposal(proposal);
        review.setFacultyUser(faculty);
        review.setDecision(decision);
        review.setComments(request.getComments());
        review.setReviewedAt(now);
        proposalReviewRepository.save(review);

        proposal.setStatus(decision);
        proposal.setUpdatedAt(now);
        proposal = proposalRepository.save(proposal);

        return toResponse(proposal);
    }

    private User resolveFaculty(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        if (!"FACULTY".equals(user.getRole())) {
            throw new ForbiddenOperationException("Only faculty can access this resource.");
        }

        return user;
    }

    private FacultyProposalResponse toResponse(ProjectProposal proposal) {
        FacultyProposalResponse response = new FacultyProposalResponse();
        response.setId(proposal.getId());
        response.setTeamId(proposal.getTeam().getId());
        response.setTeamName(proposal.getTeam().getTeamName());
        response.setTitle(proposal.getTitle());
        response.setDescription(proposal.getDescription());
        response.setObjectives(proposal.getObjectives());
        response.setTechnologyStack(proposal.getTechnologyStack());
        response.setStatus(proposal.getStatus());

        if (proposal.getSubmittedBy() != null) {
            response.setSubmittedById(proposal.getSubmittedBy().getId());
            response.setSubmittedByName(proposal.getSubmittedBy().getFullName());
        }

        response.setSubmittedAt(proposal.getSubmittedAt());
        response.setUpdatedAt(proposal.getUpdatedAt());

        proposalReviewRepository.findByProposal(proposal).ifPresent(review -> {
            response.setReviewDecision(review.getDecision());
            response.setReviewComments(review.getComments());
            response.setReviewedAt(review.getReviewedAt());
            response.setReviewedByName(review.getFacultyUser().getFullName());
        });

        return response;
    }
}