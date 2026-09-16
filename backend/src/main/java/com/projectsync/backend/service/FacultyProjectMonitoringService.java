package com.projectsync.backend.service;

import com.projectsync.backend.dto.FacultyProjectMonitoringResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.TaskRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FacultyProjectMonitoringService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public FacultyProjectMonitoringService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            UserRepository userRepository) {

        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FacultyProjectMonitoringResponse> getApprovedProjects(
            Authentication authentication) {

        resolveFaculty(authentication);

        List<Project> projects = projectRepository.findAll();

        List<FacultyProjectMonitoringResponse> responses =
                new ArrayList<>();

        for (Project project : projects) {

            if (project.getProposal() == null
                    || !"APPROVED".equalsIgnoreCase(
                            project.getProposal().getStatus())) {
                continue;
            }

            responses.add(buildMonitoringResponse(project));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public FacultyProjectMonitoringResponse getApprovedProject(
            Authentication authentication,
            java.util.UUID projectId) {

        resolveFaculty(authentication);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found."));

        if (project.getProposal() == null
                || !"APPROVED".equalsIgnoreCase(
                        project.getProposal().getStatus())) {

            throw new RuntimeException(
                    "Only projects with approved proposals can be monitored."
            );
        }

        return buildMonitoringResponse(project);
    }

    private FacultyProjectMonitoringResponse buildMonitoringResponse(
            Project project) {

        List<Task> tasks =
                taskRepository.findByProjectOrderByCreatedAtAsc(project);

        int totalTasks = tasks.size();
        int completedTasks = 0;
        int todoTasks = 0;
        int inProgressTasks = 0;
        int inReviewTasks = 0;
        int totalProgress = 0;

        for (Task task : tasks) {

            String status = task.getStatus();

            if ("TODO".equalsIgnoreCase(status)) {
                todoTasks++;
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                inProgressTasks++;
            } else if ("IN_REVIEW".equalsIgnoreCase(status)) {
                inReviewTasks++;
            } else if ("DONE".equalsIgnoreCase(status)) {
                completedTasks++;
            }

            if (task.getProgress() != null) {
                totalProgress += task.getProgress();
            }
        }

        int overallProgress = totalTasks == 0
                ? 0
                : Math.round((float) totalProgress / totalTasks);

        return new FacultyProjectMonitoringResponse(
                project.getId(),
                project.getProposal().getId(),
                project.getTeam().getId(),
                project.getName(),
                project.getStatus(),
                project.getTeam().getTeamName(),
                overallProgress,
                totalTasks,
                completedTasks,
                todoTasks,
                inProgressTasks,
                inReviewTasks
        );
    }

    private User resolveFaculty(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found."));

        if (!"FACULTY".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenOperationException(
                    "Only faculty can access project monitoring."
            );
        }

        return user;
    }
}