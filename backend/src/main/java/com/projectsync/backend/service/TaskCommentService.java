package com.projectsync.backend.service;

import com.projectsync.backend.dto.CreateTaskCommentRequest;
import com.projectsync.backend.dto.TaskCommentResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.TaskComment;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.TaskCommentRepository;
import com.projectsync.backend.repository.TaskRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskCommentService {

    private static final String APPROVED = "APPROVED";

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TaskCommentService(
            TaskCommentRepository taskCommentRepository,
            TaskRepository taskRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {

        this.taskCommentRepository = taskCommentRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskCommentResponse createComment(
            CreateTaskCommentRequest request,
            String email) {

        User user = resolveStudent(email);

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found"));

        Project project = task.getProject();

        if (project == null) {
            throw new ResourceNotFoundException(
                    "Project not found for task");
        }

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        String content = normalizeContent(request.getContent());

        TaskComment comment = new TaskComment();

        comment.setTask(task);
        comment.setUser(user);
        comment.setContent(content);

        OffsetDateTime now = OffsetDateTime.now();

        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        TaskComment saved =
                taskCommentRepository.save(comment);

        return toResponse(saved);
    }

    @Transactional
    public List<TaskCommentResponse> getTaskComments(
            UUID taskId,
            String email) {

        User user = resolveStudent(email);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Task not found"));

        Project project = task.getProject();

        if (project == null) {
            throw new ResourceNotFoundException(
                    "Project not found for task");
        }

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        return taskCommentRepository
                .findByTaskOrderByCreatedAtAsc(task)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskCommentResponse getCommentById(
            UUID commentId,
            String email) {

        User user = resolveStudent(email);

        TaskComment comment =
                taskCommentRepository.findById(commentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Task comment not found"));

        Task task = comment.getTask();

        if (task == null) {
            throw new ResourceNotFoundException(
                    "Task not found for comment");
        }

        Project project = task.getProject();

        if (project == null) {
            throw new ResourceNotFoundException(
                    "Project not found for task");
        }

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        return toResponse(comment);
    }

    @Transactional
    public TaskCommentResponse updateComment(
            UUID commentId,
            CreateTaskCommentRequest request,
            String email) {

        User user = resolveStudent(email);

        TaskComment comment =
                taskCommentRepository.findById(commentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Task comment not found"));

        Task task = comment.getTask();

        if (task == null) {
            throw new ResourceNotFoundException(
                    "Task not found for comment");
        }

        Project project = task.getProject();

        if (project == null) {
            throw new ResourceNotFoundException(
                    "Project not found for task");
        }

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException(
                    "You can only update your own comments");
        }

        if (request.getTaskId() != null
                && !request.getTaskId().equals(task.getId())) {
            throw new ConflictException(
                    "Comment cannot be moved to another task");
        }

        String content = normalizeContent(request.getContent());

        comment.setContent(content);
        comment.setUpdatedAt(OffsetDateTime.now());

        TaskComment saved =
                taskCommentRepository.save(comment);

        return toResponse(saved);
    }

    @Transactional
    public void deleteComment(
            UUID commentId,
            String email) {

        User user = resolveStudent(email);

        TaskComment comment =
                taskCommentRepository.findById(commentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Task comment not found"));

        Task task = comment.getTask();

        if (task == null) {
            throw new ResourceNotFoundException(
                    "Task not found for comment");
        }

        Project project = task.getProject();

        if (project == null) {
            throw new ResourceNotFoundException(
                    "Project not found for task");
        }

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenOperationException(
                    "You can only delete your own comments");
        }

        taskCommentRepository.delete(comment);
    }

    private User resolveStudent(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));
    }

    private void requireTeamMembership(
            Team team,
            User user) {

        teamMemberRepository
                .findByTeamAndStudentUser(team, user)
                .orElseThrow(() ->
                        new ForbiddenOperationException(
                                "You are not a member of this project team"));
    }

    private void requireApprovedProject(
            Project project) {

        if (project.getProposal() == null
                || project.getProposal().getStatus() == null
                || !APPROVED.equalsIgnoreCase(
                        project.getProposal().getStatus())) {

            throw new ForbiddenOperationException(
                    "Task comments are available only for approved projects");
        }
    }

    private String normalizeContent(String content) {

        if (content == null || content.trim().isBlank()) {
            throw new ConflictException(
                    "Comment content cannot be blank");
        }

        return content.trim();
    }

    private TaskCommentResponse toResponse(
            TaskComment comment) {

        TaskCommentResponse response =
                new TaskCommentResponse();

        response.setId(comment.getId());

        response.setTaskId(
                comment.getTask().getId());

        User user = comment.getUser();

        if (user != null) {

            response.setUserId(
                    user.getId());

            response.setUserName(
                    user.getFullName());

            response.setUserEmail(
                    user.getEmail());
        }

        response.setContent(
                comment.getContent());

        response.setCreatedAt(
                comment.getCreatedAt());

        response.setUpdatedAt(
                comment.getUpdatedAt());

        return response;
    }
}