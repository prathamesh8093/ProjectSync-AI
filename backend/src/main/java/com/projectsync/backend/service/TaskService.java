package com.projectsync.backend.service;

import com.projectsync.backend.dto.CreateTaskRequest;
import com.projectsync.backend.dto.TaskResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.SprintRepository;
import com.projectsync.backend.repository.TaskRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private static final String ROLE_LEADER = "LEADER";

    private static final String PROPOSAL_APPROVED = "APPROVED";

    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_CRITICAL = "CRITICAL";

    private static final String STATUS_TODO = "TODO";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_IN_REVIEW = "IN_REVIEW";
    private static final String STATUS_DONE = "DONE";

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskHistoryService taskHistoryService;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            SprintRepository sprintRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            TaskHistoryService taskHistoryService) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.taskHistoryService = taskHistoryService;
    }

    @Transactional
    public TaskResponse createTask(
            CreateTaskRequest request,
            String authenticatedEmail) {

        User currentUser = resolveStudent(authenticatedEmail);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Project not found"));

        Team team = project.getTeam();

        requireApprovedProject(project);
        requireTeamMembership(currentUser, team);
        requireLeaderMembership(currentUser, team);

        Sprint sprint = null;

        if (request.getSprintId() != null) {
            sprint = sprintRepository.findByIdAndProject(
                            request.getSprintId(),
                            project)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Sprint not found for this project"));
        }

        User assignedTo = null;

        if (request.getAssignedTo() != null) {
            assignedTo = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Assigned user not found"));

            requireTeamMembership(assignedTo, team);
        }

        String priority = normalizePriority(request.getPriority());
        String status = normalizeStatus(request.getStatus());

        Integer progress = request.getProgress() == null
                ? defaultProgress(status)
                : request.getProgress();

        validateStatusAndProgress(status, progress);

        Task task = new Task();

        task.setProject(project);
        task.setSprint(sprint);
        task.setAssignedTo(assignedTo);
        task.setTitle(request.getTitle().trim());
        task.setDescription(
                normalizeOptionalText(request.getDescription()));
        task.setPriority(priority);
        task.setStatus(status);
        task.setProgress(progress);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());

        Task savedTask = taskRepository.save(task);

        taskHistoryService.recordHistory(
                savedTask,
                currentUser,
                "TASK_CREATED",
                null,
                savedTask.getStatus(),
                "Task created: " + savedTask.getTitle());

        return toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyProjectTasks(
            String authenticatedEmail) {

        User currentUser = resolveStudent(authenticatedEmail);

        Team team = resolveTeam(currentUser);

        Project project = projectRepository.findByTeam(team)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No project found for your team"));

        requireApprovedProject(project);
        requireTeamMembership(currentUser, project.getTeam());

        return taskRepository
                .findByProjectOrderByCreatedAtAsc(project)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksBySprint(
            UUID sprintId,
            String authenticatedEmail) {

        User currentUser = resolveStudent(authenticatedEmail);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Sprint not found"));

        Project project = sprint.getProject();

        requireApprovedProject(project);
        requireTeamMembership(currentUser, project.getTeam());

        return taskRepository
                .findByProjectAndSprintOrderByCreatedAtAsc(
                        project,
                        sprint)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(
            UUID taskId,
            String authenticatedEmail) {

        User currentUser = resolveStudent(authenticatedEmail);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found"));

        Project project = task.getProject();

        requireApprovedProject(project);
        requireTeamMembership(currentUser, project.getTeam());

        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(
            UUID taskId,
            CreateTaskRequest request,
            String authenticatedEmail) {

        User currentUser = resolveStudent(authenticatedEmail);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found"));

        Project project = task.getProject();
        Team team = project.getTeam();

        requireApprovedProject(project);
        requireTeamMembership(currentUser, team);

        TeamMember membership =
                resolveMembership(currentUser, team);

        boolean isLeader =
                ROLE_LEADER.equals(membership.getTeamRole());

        boolean isAssignedStudent =
                task.getAssignedTo() != null
                        && task.getAssignedTo()
                        .getId()
                        .equals(currentUser.getId());

        if (!isLeader && !isAssignedStudent) {
            throw new ForbiddenOperationException(
                    "You can only update tasks assigned to you");
        }

        if (request.getProjectId() != null
                && !request.getProjectId().equals(project.getId())) {

            throw new ConflictException(
                    "Task cannot be moved to another project");
        }

        String oldSprintId =
                task.getSprint() == null
                        ? null
                        : task.getSprint().getId().toString();

        String oldAssignedToId =
                task.getAssignedTo() == null
                        ? null
                        : task.getAssignedTo().getId().toString();

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();
        String oldPriority = task.getPriority();
        String oldStatus = task.getStatus();
        Integer oldProgress = task.getProgress();

        if (request.getSprintId() != null) {

            Sprint sprint = sprintRepository.findByIdAndProject(
                            request.getSprintId(),
                            project)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Sprint not found for this project"));

            task.setSprint(sprint);

        } else if (isLeader) {
            task.setSprint(null);
        }

        if (isLeader && request.getAssignedTo() != null) {

            User assignedTo = userRepository.findById(
                            request.getAssignedTo())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Assigned user not found"));

            requireTeamMembership(assignedTo, team);

            task.setAssignedTo(assignedTo);
        }

        if (request.getTitle() != null
                && !request.getTitle().isBlank()) {

            task.setTitle(request.getTitle().trim());
        }

        if (isLeader) {

            if (request.getDescription() != null) {
                task.setDescription(
                        normalizeOptionalText(
                                request.getDescription()));
            }

            if (request.getPriority() != null) {
                task.setPriority(
                        normalizePriority(
                                request.getPriority()));
            }
        }

        if (request.getStatus() != null) {

            String status =
                    normalizeStatus(request.getStatus());

            Integer progress =
                    request.getProgress() == null
                            ? task.getProgress()
                            : request.getProgress();

            validateStatusAndProgress(
                    status,
                    progress);

            task.setStatus(status);
            task.setProgress(progress);

        } else if (request.getProgress() != null) {

            validateProgress(request.getProgress());

            task.setProgress(request.getProgress());

            if (request.getProgress() == 0) {
                task.setStatus(STATUS_TODO);
            } else if (request.getProgress() == 100) {
                task.setStatus(STATUS_DONE);
            } else if (STATUS_TODO.equals(task.getStatus())
                    || STATUS_DONE.equals(task.getStatus())) {

                task.setStatus(STATUS_IN_PROGRESS);
            }
        }

        task.setUpdatedAt(OffsetDateTime.now());

        Task savedTask = taskRepository.save(task);

        recordTaskChanges(
                savedTask,
                currentUser,
                oldSprintId,
                oldAssignedToId,
                oldTitle,
                oldDescription,
                oldPriority,
                oldStatus,
                oldProgress);

        return toResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            UUID taskId,
            String status,
            String authenticatedEmail) {

        User currentUser =
                resolveStudent(authenticatedEmail);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found"));

        Project project = task.getProject();

        requireApprovedProject(project);
        requireTeamMembership(
                currentUser,
                project.getTeam());

        boolean isLeader =
                isLeader(
                        currentUser,
                        project.getTeam());

        boolean isAssignedStudent =
                task.getAssignedTo() != null
                        && task.getAssignedTo()
                        .getId()
                        .equals(currentUser.getId());

        if (!isLeader && !isAssignedStudent) {
            throw new ForbiddenOperationException(
                    "You can only update tasks assigned to you");
        }

        String oldStatus = task.getStatus();
        Integer oldProgress = task.getProgress();

        String normalizedStatus =
                normalizeStatus(status);

        Integer progress =
                progressForStatus(normalizedStatus);

        validateStatusAndProgress(
                normalizedStatus,
                progress);

        task.setStatus(normalizedStatus);
        task.setProgress(progress);
        task.setUpdatedAt(OffsetDateTime.now());

        Task savedTask = taskRepository.save(task);

        if (!oldStatus.equals(savedTask.getStatus())) {
            taskHistoryService.recordHistory(
                    savedTask,
                    currentUser,
                    "STATUS_CHANGED",
                    oldStatus,
                    savedTask.getStatus(),
                    "Task status changed from "
                            + oldStatus
                            + " to "
                            + savedTask.getStatus());
        }

        if (!oldProgress.equals(savedTask.getProgress())) {
            taskHistoryService.recordHistory(
                    savedTask,
                    currentUser,
                    "PROGRESS_CHANGED",
                    String.valueOf(oldProgress),
                    String.valueOf(savedTask.getProgress()),
                    "Task progress changed from "
                            + oldProgress
                            + "% to "
                            + savedTask.getProgress()
                            + "%");
        }

        return toResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTaskProgress(
            UUID taskId,
            Integer progress,
            String authenticatedEmail) {

        User currentUser =
                resolveStudent(authenticatedEmail);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found"));

        Project project = task.getProject();

        requireApprovedProject(project);
        requireTeamMembership(
                currentUser,
                project.getTeam());

        boolean isLeader =
                isLeader(
                        currentUser,
                        project.getTeam());

        boolean isAssignedStudent =
                task.getAssignedTo() != null
                        && task.getAssignedTo()
                        .getId()
                        .equals(currentUser.getId());

        if (!isLeader && !isAssignedStudent) {
            throw new ForbiddenOperationException(
                    "You can only update tasks assigned to you");
        }

        validateProgress(progress);

        String oldStatus = task.getStatus();
        Integer oldProgress = task.getProgress();

        task.setProgress(progress);

        if (progress == 0) {

            task.setStatus(STATUS_TODO);

        } else if (progress == 100) {

            task.setStatus(STATUS_DONE);

        } else if (STATUS_TODO.equals(task.getStatus())
                || STATUS_DONE.equals(task.getStatus())) {

            task.setStatus(STATUS_IN_PROGRESS);
        }

        task.setUpdatedAt(OffsetDateTime.now());

        Task savedTask = taskRepository.save(task);

        if (!oldProgress.equals(savedTask.getProgress())) {
            taskHistoryService.recordHistory(
                    savedTask,
                    currentUser,
                    "PROGRESS_CHANGED",
                    String.valueOf(oldProgress),
                    String.valueOf(savedTask.getProgress()),
                    "Task progress changed from "
                            + oldProgress
                            + "% to "
                            + savedTask.getProgress()
                            + "%");
        }

        if (!oldStatus.equals(savedTask.getStatus())) {
            taskHistoryService.recordHistory(
                    savedTask,
                    currentUser,
                    "STATUS_CHANGED",
                    oldStatus,
                    savedTask.getStatus(),
                    "Task status changed from "
                            + oldStatus
                            + " to "
                            + savedTask.getStatus());
        }

        return toResponse(savedTask);
    }

    private User resolveStudent(
            String authenticatedEmail) {

        User user = userRepository
                .findByEmail(authenticatedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));

        if (!"STUDENT".equals(user.getRole())) {
            throw new ForbiddenOperationException(
                    "Only students can access this resource");
        }

        return user;
    }

    private Team resolveTeam(User user) {

        TeamMember membership =
                teamMemberRepository
                        .findByStudentUser(user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "You are not a member of any team"));

        return membership.getTeam();
    }

    private TeamMember resolveMembership(
            User user,
            Team team) {

        return teamMemberRepository
                .findByTeamAndStudentUser(team, user)
                .orElseThrow(() ->
                        new ForbiddenOperationException(
                                "You are not a member of this team"));
    }

    private void requireTeamMembership(
            User user,
            Team team) {

        if (!teamMemberRepository
                .findByTeamAndStudentUser(team, user)
                .isPresent()) {

            throw new ForbiddenOperationException(
                    "You are not a member of this project team");
        }
    }

    private void requireLeaderMembership(
            User user,
            Team team) {

        TeamMember membership =
                resolveMembership(user, team);

        if (!ROLE_LEADER.equals(
                membership.getTeamRole())) {

            throw new ForbiddenOperationException(
                    "Only the team leader can perform this action");
        }
    }

    private boolean isLeader(
            User user,
            Team team) {

        return teamMemberRepository
                .findByTeamAndStudentUser(team, user)
                .map(member ->
                        ROLE_LEADER.equals(
                                member.getTeamRole()))
                .orElse(false);
    }

    private void requireApprovedProject(
            Project project) {

        ProjectProposal proposal =
                project.getProposal();

        if (proposal == null) {
            throw new ConflictException(
                    "Project is not linked to a proposal");
        }

        if (!PROPOSAL_APPROVED.equalsIgnoreCase(
                proposal.getStatus())) {

            throw new ForbiddenOperationException(
                    "Agile tasks are available only for approved projects");
        }
    }

    private String normalizePriority(
            String priority) {

        if (priority == null
                || priority.isBlank()) {

            return PRIORITY_MEDIUM;
        }

        String normalized =
                priority.trim().toUpperCase();

        if (!PRIORITY_LOW.equals(normalized)
                && !PRIORITY_MEDIUM.equals(normalized)
                && !PRIORITY_HIGH.equals(normalized)
                && !PRIORITY_CRITICAL.equals(normalized)) {

            throw new ConflictException(
                    "Invalid task priority. Allowed values: LOW, MEDIUM, HIGH, CRITICAL");
        }

        return normalized;
    }

    private String normalizeStatus(
            String status) {

        if (status == null
                || status.isBlank()) {

            return STATUS_TODO;
        }

        String normalized =
                status.trim().toUpperCase();

        if (!STATUS_TODO.equals(normalized)
                && !STATUS_IN_PROGRESS.equals(normalized)
                && !STATUS_IN_REVIEW.equals(normalized)
                && !STATUS_DONE.equals(normalized)) {

            throw new ConflictException(
                    "Invalid task status. Allowed values: TODO, IN_PROGRESS, IN_REVIEW, DONE");
        }

        return normalized;
    }

    private Integer defaultProgress(
            String status) {

        return progressForStatus(status);
    }

    private Integer progressForStatus(
            String status) {

        return switch (status) {

            case STATUS_TODO -> 0;
            case STATUS_IN_PROGRESS -> 50;
            case STATUS_IN_REVIEW -> 75;
            case STATUS_DONE -> 100;
            default -> 0;
        };
    }

    private void validateProgress(
            Integer progress) {

        if (progress == null
                || progress < 0
                || progress > 100) {

            throw new ConflictException(
                    "Task progress must be between 0 and 100");
        }
    }

    private void validateStatusAndProgress(
            String status,
            Integer progress) {

        validateProgress(progress);

        if (STATUS_TODO.equals(status)
                && progress != 0) {

            throw new ConflictException(
                    "TODO tasks must have 0% progress");
        }

        if (STATUS_DONE.equals(status)
                && progress != 100) {

            throw new ConflictException(
                    "DONE tasks must have 100% progress");
        }

        if ((STATUS_IN_PROGRESS.equals(status)
                || STATUS_IN_REVIEW.equals(status))
                && (progress <= 0
                || progress >= 100)) {

            throw new ConflictException(
                    status
                            + " tasks must have progress between 1 and 99");
        }
    }

    private void recordTaskChanges(
            Task task,
            User currentUser,
            String oldSprintId,
            String oldAssignedToId,
            String oldTitle,
            String oldDescription,
            String oldPriority,
            String oldStatus,
            Integer oldProgress) {

        String newSprintId =
                task.getSprint() == null
                        ? null
                        : task.getSprint().getId().toString();

        if (!java.util.Objects.equals(oldSprintId, newSprintId)) {
            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "SPRINT_CHANGED",
                    oldSprintId,
                    newSprintId,
                    "Task sprint assignment changed");
        }

        String newAssignedToId =
                task.getAssignedTo() == null
                        ? null
                        : task.getAssignedTo().getId().toString();

        if (!java.util.Objects.equals(
                oldAssignedToId,
                newAssignedToId)) {

            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "ASSIGNMENT_CHANGED",
                    oldAssignedToId,
                    newAssignedToId,
                    "Task assignment changed");
        }

        if (!java.util.Objects.equals(oldTitle, task.getTitle())) {
            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "TITLE_CHANGED",
                    oldTitle,
                    task.getTitle(),
                    "Task title changed");
        }

        if (!java.util.Objects.equals(
                oldDescription,
                task.getDescription())) {

            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "DESCRIPTION_CHANGED",
                    oldDescription,
                    task.getDescription(),
                    "Task description changed");
        }

        if (!java.util.Objects.equals(
                oldPriority,
                task.getPriority())) {

            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "PRIORITY_CHANGED",
                    oldPriority,
                    task.getPriority(),
                    "Task priority changed from "
                            + oldPriority
                            + " to "
                            + task.getPriority());
        }

        if (!java.util.Objects.equals(
                oldStatus,
                task.getStatus())) {

            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "STATUS_CHANGED",
                    oldStatus,
                    task.getStatus(),
                    "Task status changed from "
                            + oldStatus
                            + " to "
                            + task.getStatus());
        }

        if (!java.util.Objects.equals(
                oldProgress,
                task.getProgress())) {

            taskHistoryService.recordHistory(
                    task,
                    currentUser,
                    "PROGRESS_CHANGED",
                    oldProgress == null
                            ? null
                            : String.valueOf(oldProgress),
                    task.getProgress() == null
                            ? null
                            : String.valueOf(task.getProgress()),
                    "Task progress changed from "
                            + oldProgress
                            + "% to "
                            + task.getProgress()
                            + "%");
        }
    }

    private String normalizeOptionalText(
            String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private TaskResponse toResponse(
            Task task) {

        TaskResponse response =
                new TaskResponse();

        response.setId(task.getId());

        response.setProjectId(
                task.getProject().getId());

        if (task.getSprint() != null) {
            response.setSprintId(
                    task.getSprint().getId());
        }

        if (task.getAssignedTo() != null) {

            response.setAssignedTo(
                    task.getAssignedTo().getId());

            response.setAssignedToName(
                    task.getAssignedTo().getFullName());

            response.setAssignedToEmail(
                    task.getAssignedTo().getEmail());
        }

        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        response.setProgress(task.getProgress());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        return response;
    }
}