package com.projectsync.backend.service;

import com.projectsync.backend.dto.CreateProposalRequest;
import com.projectsync.backend.dto.ProjectProposalResponse;
import com.projectsync.backend.dto.UpdateProposalRequest;
import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectProposalRepository;
import com.projectsync.backend.repository.ProposalReviewRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ProjectProposalService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";

    private final ProjectProposalRepository proposalRepository;
    private final ProposalReviewRepository proposalReviewRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ProjectCodeGenerator projectCodeGenerator;

    public ProjectProposalService(ProjectProposalRepository proposalRepository,
                                   ProposalReviewRepository proposalReviewRepository,
                                   TeamMemberRepository teamMemberRepository,
                                   UserRepository userRepository,
                                   ProjectCodeGenerator projectCodeGenerator) {
        this.proposalRepository = proposalRepository;
        this.proposalReviewRepository = proposalReviewRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.projectCodeGenerator = projectCodeGenerator;
    }

    @Transactional
    public ProjectProposalResponse createProposal(Authentication authentication, CreateProposalRequest request) {
        TeamMember leaderMembership = resolveLeaderMembership(authentication);
        Team team = leaderMembership.getTeam();

        if (proposalRepository.existsByTeam(team)) {
            throw new ConflictException("Your team already has a proposal.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        ProjectProposal proposal = new ProjectProposal();
        proposal.setTeam(team);
        proposal.setTitle(request.getTitle());
        proposal.setDescription(request.getDescription());
        proposal.setObjectives(request.getObjectives());
        proposal.setTechnologyStack(request.getTechnologyStack());
        proposal.setStatus(STATUS_DRAFT);
        proposal.setProjectCode(projectCodeGenerator.generateNextProjectCode());
        proposal.setSubmittedBy(null);
        proposal.setSubmittedAt(null);
        proposal.setUpdatedAt(now);

        proposal = proposalRepository.save(proposal);

        return toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public ProjectProposalResponse getMyProposal(Authentication authentication) {
        User currentUser = resolveStudent(authentication);
        TeamMember membership = requireTeamMembership(currentUser);

        ProjectProposal proposal = proposalRepository.findByTeam(membership.getTeam())
                .orElseThrow(() -> new ResourceNotFoundException("Your team has not created a proposal yet."));

        return toResponse(proposal);
    }

    @Transactional
    public ProjectProposalResponse updateProposal(Authentication authentication,
                                                    UUID proposalId,
                                                    UpdateProposalRequest request) {
        TeamMember leaderMembership = resolveLeaderMembership(authentication);
        ProjectProposal proposal = findProposalForTeam(proposalId, leaderMembership.getTeam());

        if (!STATUS_DRAFT.equals(proposal.getStatus())) {
            throw new ConflictException("Only proposals in DRAFT status can be edited.");
        }

        proposal.setTitle(request.getTitle());
        proposal.setDescription(request.getDescription());
        proposal.setObjectives(request.getObjectives());
        proposal.setTechnologyStack(request.getTechnologyStack());
        proposal.setUpdatedAt(OffsetDateTime.now());

        proposal = proposalRepository.save(proposal);

        return toResponse(proposal);
    }

    @Transactional
    public ProjectProposalResponse submitProposal(Authentication authentication, UUID proposalId) {
        TeamMember leaderMembership = resolveLeaderMembership(authentication);
        ProjectProposal proposal = findProposalForTeam(proposalId, leaderMembership.getTeam());

        if (!STATUS_DRAFT.equals(proposal.getStatus())) {
            throw new ConflictException("Only proposals in DRAFT status can be submitted.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        proposal.setStatus(STATUS_SUBMITTED);
        proposal.setSubmittedBy(leaderMembership.getStudentUser());
        proposal.setSubmittedAt(now);
        proposal.setUpdatedAt(now);

        proposal = proposalRepository.save(proposal);

        return toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public ProjectProposalResponse getProposalById(Authentication authentication, UUID proposalId) {
        User currentUser = resolveStudent(authentication);
        TeamMember membership = requireTeamMembership(currentUser);

        ProjectProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found."));

        if (!proposal.getTeam().getId().equals(membership.getTeam().getId())) {
            throw new ForbiddenOperationException("You do not have access to this proposal.");
        }

        return toResponse(proposal);
    }

    private ProjectProposal findProposalForTeam(UUID proposalId, Team team) {
        ProjectProposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found."));

        if (!proposal.getTeam().getId().equals(team.getId())) {
            throw new ForbiddenOperationException("You do not have access to this proposal.");
        }

        return proposal;
    }

    private TeamMember resolveLeaderMembership(Authentication authentication) {
        User currentUser = resolveStudent(authentication);
        TeamMember membership = requireTeamMembership(currentUser);

        if (!"LEADER".equals(membership.getTeamRole())) {
            throw new ForbiddenOperationException("Only the team leader can perform this action.");
        }

        return membership;
    }

    private TeamMember requireTeamMembership(User user) {
        return teamMemberRepository.findByStudentUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("You are not part of a team yet."));
    }

    private User resolveStudent(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        if (!"STUDENT".equals(user.getRole())) {
            throw new ForbiddenOperationException("Only students can access this resource.");
        }

        return user;
    }

    private ProjectProposalResponse toResponse(ProjectProposal proposal) {
        ProjectProposalResponse response = new ProjectProposalResponse();
        response.setId(proposal.getId());
        response.setTeamId(proposal.getTeam().getId());
        response.setTitle(proposal.getTitle());
        response.setDescription(proposal.getDescription());
        response.setObjectives(proposal.getObjectives());
        response.setTechnologyStack(proposal.getTechnologyStack());
        response.setStatus(proposal.getStatus());

        if (proposal.getProjectCode() == null || proposal.getProjectCode().isBlank()) {
            proposal.setProjectCode(projectCodeGenerator.generateNextProjectCode());
            proposal = proposalRepository.save(proposal);
        }
        response.setProjectCode(proposal.getProjectCode());

        response.setSubmittedBy(proposal.getSubmittedBy() != null ? proposal.getSubmittedBy().getId() : null);
        response.setSubmittedAt(proposal.getSubmittedAt());
        response.setUpdatedAt(proposal.getUpdatedAt());

        proposalReviewRepository.findByProposal(proposal).ifPresent(review -> {
            response.setReviewDecision(review.getDecision());
            response.setReviewComments(review.getComments());
            response.setReviewedAt(review.getReviewedAt());
            if (review.getFacultyUser() != null) {
                response.setReviewedByName(review.getFacultyUser().getFullName());
            }
        });

        return response;
    }
}