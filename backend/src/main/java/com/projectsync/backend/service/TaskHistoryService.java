package com.projectsync.backend.service;

import com.projectsync.backend.dto.TaskHistoryResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.TaskHistory;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.TaskHistoryRepository;
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
public class TaskHistoryService {

    private static final String APPROVED = "APPROVED";

    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TaskHistoryService(
            TaskHistoryRepository taskHistoryRepository,
            TaskRepository taskRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {

        this.taskHistoryRepository = taskHistoryRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<TaskHistoryResponse> getTaskHistory(
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

        return taskHistoryRepository
                .findByTaskOrderByCreatedAtDesc(task)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskHistoryResponse getHistoryById(
            UUID historyId,
            String email) {

        User user = resolveStudent(email);

        TaskHistory history =
                taskHistoryRepository.findById(historyId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Task history entry not found"));

        Task task = history.getTask();

        if (task == null) {
            throw new ResourceNotFoundException(
                    "Task not found for history entry");
        }

        Project project = task.getProject();

        if (project == null) {
            throw new ResourceNotFoundException(
                    "Project not found for task");
        }

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        return toResponse(history);
    }

    @Transactional
    public TaskHistoryResponse recordHistory(
            Task task,
            User user,
            String actionType,
            String oldValue,
            String newValue,
            String description) {

        if (task == null) {
            throw new ResourceNotFoundException(
                    "Task is required for history");
        }

        if (user == null) {
            throw new ResourceNotFoundException(
                    "User is required for history");
        }

        if (actionType == null
                || actionType.trim().isBlank()) {
            throw new IllegalArgumentException(
                    "Action type is required");
        }

        if (description == null
                || description.trim().isBlank()) {
            throw new IllegalArgumentException(
                    "History description is required");
        }

        TaskHistory history = new TaskHistory();

        history.setTask(task);
        history.setUser(user);
        history.setActionType(actionType.trim());
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setDescription(description.trim());
        history.setCreatedAt(OffsetDateTime.now());

        TaskHistory saved =
                taskHistoryRepository.save(history);

        return toResponse(saved);
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
                    "Task history is available only for approved projects");
        }
    }

    private TaskHistoryResponse toResponse(
            TaskHistory history) {

        TaskHistoryResponse response =
                new TaskHistoryResponse();

        response.setId(history.getId());

        response.setTaskId(
                history.getTask().getId());

        User user = history.getUser();

        if (user != null) {
            response.setUserId(
                    user.getId());

            response.setUserName(
                    user.getFullName());

            response.setUserEmail(
                    user.getEmail());
        }

        response.setActionType(
                history.getActionType());

        response.setOldValue(
                history.getOldValue());

        response.setNewValue(
                history.getNewValue());

        response.setDescription(
                history.getDescription());

        response.setCreatedAt(
                history.getCreatedAt());

        return response;
    }
}