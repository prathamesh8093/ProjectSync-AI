package com.projectsync.backend.service;

import com.projectsync.backend.dto.CreateProjectRequest;
import com.projectsync.backend.dto.ProjectProgressResponse;
import com.projectsync.backend.dto.ProjectResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectProposalRepository;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.TaskRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private static final String PROPOSAL_STATUS_APPROVED = "APPROVED";
    private static final String PROJECT_STATUS_NOT_STARTED = "NOT_STARTED";

    private final ProjectRepository projectRepository;
    private final ProjectProposalRepository proposalRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectProposalRepository proposalRepository,
            TaskRepository taskRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.proposalRepository = proposalRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectResponse createProject(Authentication authentication, CreateProjectRequest request) {
        TeamMember leaderMembership = resolveLeaderMembership(authentication);
        Team team = leaderMembership.getTeam();

        ProjectProposal proposal = proposalRepository.findById(request.getProposalId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found."));

        if (!proposal.getTeam().getId().equals(team.getId())) {
            throw new ForbiddenOperationException("You do not have access to this proposal.");
        }
        if (!PROPOSAL_STATUS_APPROVED.equals(proposal.getStatus())) {
            throw new ConflictException("An Agile project can only be created for an approved proposal.");
        }
        if (projectRepository.existsByProposal(proposal)) {
            throw new ConflictException("An Agile project already exists for this proposal.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Project project = new Project();
        project.setTeam(team);
        project.setProposal(proposal);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setStatus(PROJECT_STATUS_NOT_STARTED);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);

        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getMyProject(Authentication authentication) {
        User currentUser = resolveStudent(authentication);
        TeamMember membership = requireTeamMembership(currentUser);

        Project project = projectRepository.findByTeam(membership.getTeam())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Your team has not created an Agile project yet."));

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectProgressResponse getMyProjectProgress(Authentication authentication) {
        User currentUser = resolveStudent(authentication);
        TeamMember membership = requireTeamMembership(currentUser);

        Project project = projectRepository.findByTeam(membership.getTeam())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Your team has not created an Agile project yet."));

        requireApprovedProject(project);
        requireTeamMembership(currentUser, project.getTeam());

        List<Task> tasks = taskRepository.findByProjectOrderByCreatedAtAsc(project);

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int todoTasks = 0;
        int inProgressTasks = 0;
        int inReviewTasks = 0;
        int totalProgress = 0;

        for (Task task : tasks) {
            int progress = task.getProgress() == null ? 0 : task.getProgress();
            totalProgress += progress;

            if ("DONE".equals(task.getStatus())) {
                completedTasks++;
            } else if ("TODO".equals(task.getStatus())) {
                todoTasks++;
            } else if ("IN_PROGRESS".equals(task.getStatus())) {
                inProgressTasks++;
            } else if ("IN_REVIEW".equals(task.getStatus())) {
                inReviewTasks++;
            }
        }

        int overallProgress = totalTasks == 0
                ? 0
                : (int) Math.round(totalProgress / (double) totalTasks);

        ProjectProgressResponse response = new ProjectProgressResponse();
        response.setProjectId(project.getId());
        response.setProjectName(project.getName());
        response.setProjectStatus(project.getStatus());
        response.setOverallProgress(overallProgress);
        response.setTotalTasks(totalTasks);
        response.setCompletedTasks(completedTasks);
        response.setTodoTasks(todoTasks);
        response.setInProgressTasks(inProgressTasks);
        response.setInReviewTasks(inReviewTasks);

        return response;
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Authentication authentication, UUID projectId) {
        User currentUser = resolveStudent(authentication);
        TeamMember membership = requireTeamMembership(currentUser);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (!project.getTeam().getId().equals(membership.getTeam().getId())) {
            throw new ForbiddenOperationException("You do not have access to this project.");
        }

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(
            Authentication authentication,
            UUID projectId,
            CreateProjectRequest request) {
        TeamMember leaderMembership = resolveLeaderMembership(authentication);
        Project project = findProjectForTeam(projectId, leaderMembership.getTeam());

        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setUpdatedAt(OffsetDateTime.now());

        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProjectStatus(
            Authentication authentication,
            UUID projectId,
            String status) {
        TeamMember leaderMembership = resolveLeaderMembership(authentication);
        Project project = findProjectForTeam(projectId, leaderMembership.getTeam());

        if (!"NOT_STARTED".equals(status)
                && !"ACTIVE".equals(status)
                && !"COMPLETED".equals(status)) {
            throw new ConflictException("Invalid project status.");
        }

        project.setStatus(status);
        project.setUpdatedAt(OffsetDateTime.now());
        project = projectRepository.save(project);
        return toResponse(project);
    }

    private Project findProjectForTeam(UUID projectId, Team team) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (!project.getTeam().getId().equals(team.getId())) {
            throw new ForbiddenOperationException("You do not have access to this project.");
        }
        return project;
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not part of a team yet."));
    }

    private void requireTeamMembership(User user, Team team) {
        teamMemberRepository.findByTeamAndStudentUser(team, user)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "You do not have access to this project."));
    }

    private void requireApprovedProject(Project project) {
        if (!PROPOSAL_STATUS_APPROVED.equals(project.getProposal().getStatus())) {
            throw new ConflictException(
                    "Agile project progress is only available for an approved proposal.");
        }
    }

    private User resolveStudent(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found."));

        if (!"STUDENT".equals(user.getRole())) {
            throw new ForbiddenOperationException(
                    "Only students can access this resource.");
        }
        return user;
    }

    private ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setTeamId(project.getTeam().getId());
        response.setProposalId(project.getProposal().getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setStatus(project.getStatus());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
