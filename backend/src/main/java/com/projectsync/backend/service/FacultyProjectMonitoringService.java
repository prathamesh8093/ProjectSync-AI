package com.projectsync.backend.service;

import com.projectsync.backend.dto.FacultyProjectMonitoringResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.SprintRepository;
import com.projectsync.backend.repository.TaskRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FacultyProjectMonitoringService {

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public FacultyProjectMonitoringService(
            ProjectRepository projectRepository,
            SprintRepository sprintRepository,
            TaskRepository taskRepository,
            UserRepository userRepository) {

        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
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
            UUID projectId) {

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

        FacultyProjectMonitoringResponse response =
                new FacultyProjectMonitoringResponse(
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

        String code = project.getProjectCode();
        if (code == null && project.getProposal() != null) {
            code = project.getProposal().getProjectCode();
        }
        response.setProjectCode(code);

        response.setSprints(buildSprintMonitoringResponses(project));
        response.setTasks(buildTaskMonitoringResponses(tasks));

        return response;
    }

    private List<FacultyProjectMonitoringResponse.SprintMonitoringResponse>
    buildSprintMonitoringResponses(Project project) {

        List<Sprint> sprints =
                sprintRepository.findByProjectOrderByStartDateAsc(project);

        List<FacultyProjectMonitoringResponse.SprintMonitoringResponse>
                responses = new ArrayList<>();

        for (Sprint sprint : sprints) {

            List<Task> sprintTasks =
                    taskRepository.findByProjectAndSprintOrderByCreatedAtAsc(
                            project,
                            sprint
                    );

            int totalTasks = sprintTasks.size();
            int completedTasks = 0;
            int todoTasks = 0;
            int inProgressTasks = 0;
            int inReviewTasks = 0;
            int totalProgress = 0;

            for (Task task : sprintTasks) {

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

            int progress = totalTasks == 0
                    ? 0
                    : Math.round((float) totalProgress / totalTasks);

            responses.add(
                    new FacultyProjectMonitoringResponse
                            .SprintMonitoringResponse(
                                    sprint.getId(),
                                    sprint.getName(),
                                    sprint.getGoal(),
                                    sprint.getStartDate(),
                                    sprint.getEndDate(),
                                    sprint.getStatus(),
                                    totalTasks,
                                    completedTasks,
                                    todoTasks,
                                    inProgressTasks,
                                    inReviewTasks,
                                    progress
                            )
            );
        }

        return responses;
    }

    private List<FacultyProjectMonitoringResponse.TaskMonitoringResponse>
    buildTaskMonitoringResponses(List<Task> tasks) {

        List<FacultyProjectMonitoringResponse.TaskMonitoringResponse>
                responses = new ArrayList<>();

        for (Task task : tasks) {

            Sprint sprint = task.getSprint();
            User assignedTo = task.getAssignedTo();

            String sprintName = sprint == null
                    ? null
                    : sprint.getName();

            String assignedToName = assignedTo == null
                    ? null
                    : assignedTo.getFullName();

            int progress = task.getProgress() == null
                    ? 0
                    : task.getProgress();

            responses.add(
                    new FacultyProjectMonitoringResponse
                            .TaskMonitoringResponse(
                                    task.getId(),
                                    task.getTitle(),
                                    task.getDescription(),
                                    sprintName,
                                    assignedToName,
                                    task.getPriority(),
                                    task.getStatus(),
                                    progress
                            )
            );
        }

        return responses;
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