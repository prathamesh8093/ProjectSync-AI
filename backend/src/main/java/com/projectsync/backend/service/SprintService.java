package com.projectsync.backend.service;

import com.projectsync.backend.dto.CreateSprintRequest;
import com.projectsync.backend.dto.SprintResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.SprintRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SprintService {

    private static final String PROPOSAL_STATUS_APPROVED = "APPROVED";

    private static final String SPRINT_STATUS_PLANNED = "PLANNED";
    private static final String SPRINT_STATUS_ACTIVE = "ACTIVE";
    private static final String SPRINT_STATUS_COMPLETED = "COMPLETED";

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public SprintService(
            SprintRepository sprintRepository,
            ProjectRepository projectRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {

        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SprintResponse createSprint(
            Authentication authentication,
            CreateSprintRequest request) {

        TeamMember leaderMembership =
                resolveLeaderMembership(authentication);

        Team team = leaderMembership.getTeam();

        Project project = findProjectForTeam(
                request.getProjectId(),
                team);

        ensureApprovedProject(project);

        validateDates(
                request.getStartDate(),
                request.getEndDate());

        if (sprintRepository.existsByProjectAndStatus(
                project,
                SPRINT_STATUS_ACTIVE)) {

            throw new ConflictException(
                    "This project already has an active sprint.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        Sprint sprint = new Sprint();

        sprint.setProject(project);
        sprint.setName(request.getName().trim());
        sprint.setGoal(normalizeOptionalText(request.getGoal()));
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setStatus(SPRINT_STATUS_PLANNED);
        sprint.setCreatedAt(now);
        sprint.setUpdatedAt(now);

        sprint = sprintRepository.save(sprint);

        return toResponse(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getMyProjectSprints(
            Authentication authentication) {

        User currentUser = resolveStudent(authentication);

        TeamMember membership =
                requireTeamMembership(currentUser);

        Project project = projectRepository.findByTeam(
                        membership.getTeam())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Your team has not created an Agile project yet."));

        ensureApprovedProject(project);

        return sprintRepository
                .findByProjectOrderByStartDateAsc(project)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SprintResponse getSprintById(
            Authentication authentication,
            UUID sprintId) {

        User currentUser = resolveStudent(authentication);

        TeamMember membership =
                requireTeamMembership(currentUser);

        Project project = projectRepository.findByTeam(
                        membership.getTeam())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Your team has not created an Agile project yet."));

        ensureApprovedProject(project);

        Sprint sprint = sprintRepository
                .findByIdAndProject(sprintId, project)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sprint not found."));

        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse updateSprint(
            Authentication authentication,
            UUID sprintId,
            CreateSprintRequest request) {

        TeamMember leaderMembership =
                resolveLeaderMembership(authentication);

        Team team = leaderMembership.getTeam();

        Sprint sprint = findSprintForTeam(
                sprintId,
                team);

        Project project = sprint.getProject();

        ensureApprovedProject(project);

        validateDates(
                request.getStartDate(),
                request.getEndDate());

        sprint.setName(request.getName().trim());
        sprint.setGoal(normalizeOptionalText(request.getGoal()));
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setUpdatedAt(OffsetDateTime.now());

        sprint = sprintRepository.save(sprint);

        return toResponse(sprint);
    }

    @Transactional
    public SprintResponse updateSprintStatus(
            Authentication authentication,
            UUID sprintId,
            String status) {

        TeamMember leaderMembership =
                resolveLeaderMembership(authentication);

        Team team = leaderMembership.getTeam();

        Sprint sprint = findSprintForTeam(
                sprintId,
                team);

        Project project = sprint.getProject();

        ensureApprovedProject(project);

        validateStatus(status);

        if (SPRINT_STATUS_ACTIVE.equals(status)
                && !SPRINT_STATUS_ACTIVE.equals(sprint.getStatus())
                && sprintRepository.existsByProjectAndStatusAndIdNot(
                        project,
                        SPRINT_STATUS_ACTIVE,
                        sprint.getId())) {

            throw new ConflictException(
                    "This project already has an active sprint.");
        }

        sprint.setStatus(status);
        sprint.setUpdatedAt(OffsetDateTime.now());

        sprint = sprintRepository.save(sprint);

        return toResponse(sprint);
    }

    private Project findProjectForTeam(
            UUID projectId,
            Team team) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found."));

        if (!project.getTeam().getId().equals(team.getId())) {
            throw new ForbiddenOperationException(
                    "You do not have access to this project.");
        }

        return project;
    }

    private Sprint findSprintForTeam(
            UUID sprintId,
            Team team) {

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Sprint not found."));

        if (!sprint.getProject()
                .getTeam()
                .getId()
                .equals(team.getId())) {

            throw new ForbiddenOperationException(
                    "You do not have access to this sprint.");
        }

        return sprint;
    }

    private void ensureApprovedProject(Project project) {

        if (project.getProposal() == null
                || !PROPOSAL_STATUS_APPROVED.equals(
                        project.getProposal().getStatus())) {

            throw new ConflictException(
                    "Sprint management is only available for an approved proposal.");
        }
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new ConflictException(
                    "Sprint end date cannot be before the start date.");
        }
    }

    private void validateStatus(String status) {

        if (!SPRINT_STATUS_PLANNED.equals(status)
                && !SPRINT_STATUS_ACTIVE.equals(status)
                && !SPRINT_STATUS_COMPLETED.equals(status)) {

            throw new ConflictException(
                    "Invalid sprint status.");
        }
    }

    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private TeamMember resolveLeaderMembership(
            Authentication authentication) {

        User currentUser = resolveStudent(authentication);

        TeamMember membership =
                requireTeamMembership(currentUser);

        if (!"LEADER".equals(membership.getTeamRole())) {
            throw new ForbiddenOperationException(
                    "Only the team leader can perform this action.");
        }

        return membership;
    }

    private TeamMember requireTeamMembership(User user) {

        return teamMemberRepository.findByStudentUser(user)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "You are not part of a team yet."));
    }

    private User resolveStudent(
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found."));

        if (!"STUDENT".equals(user.getRole())) {
            throw new ForbiddenOperationException(
                    "Only students can access this resource.");
        }

        return user;
    }

    private SprintResponse toResponse(Sprint sprint) {

        SprintResponse response = new SprintResponse();

        response.setId(sprint.getId());
        response.setProjectId(
                sprint.getProject().getId());
        response.setName(sprint.getName());
        response.setGoal(sprint.getGoal());
        response.setStartDate(sprint.getStartDate());
        response.setEndDate(sprint.getEndDate());
        response.setStatus(sprint.getStatus());
        response.setCreatedAt(sprint.getCreatedAt());
        response.setUpdatedAt(sprint.getUpdatedAt());

        return response;
    }
}