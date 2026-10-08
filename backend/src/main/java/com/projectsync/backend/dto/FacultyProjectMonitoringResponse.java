package com.projectsync.backend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FacultyProjectMonitoringResponse {

    private UUID projectId;
    private UUID proposalId;
    private UUID teamId;
    private String projectCode;

    private String projectName;
    private String projectStatus;
    private String teamName;

    private int overallProgress;
    private int totalTasks;
    private int completedTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int inReviewTasks;

    private List<SprintMonitoringResponse> sprints = new ArrayList<>();
    private List<TaskMonitoringResponse> tasks = new ArrayList<>();

    public FacultyProjectMonitoringResponse() {
    }

    public FacultyProjectMonitoringResponse(
            UUID projectId,
            UUID proposalId,
            UUID teamId,
            String projectName,
            String projectStatus,
            String teamName,
            int overallProgress,
            int totalTasks,
            int completedTasks,
            int todoTasks,
            int inProgressTasks,
            int inReviewTasks) {

        this.projectId = projectId;
        this.proposalId = proposalId;
        this.teamId = teamId;
        this.projectName = projectName;
        this.projectStatus = projectStatus;
        this.teamName = teamName;
        this.overallProgress = overallProgress;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.todoTasks = todoTasks;
        this.inProgressTasks = inProgressTasks;
        this.inReviewTasks = inReviewTasks;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public void setProposalId(UUID proposalId) {
        this.proposalId = proposalId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(int overallProgress) {
        this.overallProgress = overallProgress;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getTodoTasks() {
        return todoTasks;
    }

    public void setTodoTasks(int todoTasks) {
        this.todoTasks = todoTasks;
    }

    public int getInProgressTasks() {
        return inProgressTasks;
    }

    public void setInProgressTasks(int inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public int getInReviewTasks() {
        return inReviewTasks;
    }

    public void setInReviewTasks(int inReviewTasks) {
        this.inReviewTasks = inReviewTasks;
    }

    public List<SprintMonitoringResponse> getSprints() {
        return sprints;
    }

    public void setSprints(List<SprintMonitoringResponse> sprints) {
        this.sprints = sprints;
    }

    public List<TaskMonitoringResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskMonitoringResponse> tasks) {
        this.tasks = tasks;
    }

    public static class SprintMonitoringResponse {

        private UUID sprintId;
        private String name;
        private String goal;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;

        private int totalTasks;
        private int completedTasks;
        private int todoTasks;
        private int inProgressTasks;
        private int inReviewTasks;
        private int progress;

        public SprintMonitoringResponse() {
        }

        public SprintMonitoringResponse(
                UUID sprintId,
                String name,
                String goal,
                LocalDate startDate,
                LocalDate endDate,
                String status,
                int totalTasks,
                int completedTasks,
                int todoTasks,
                int inProgressTasks,
                int inReviewTasks,
                int progress) {

            this.sprintId = sprintId;
            this.name = name;
            this.goal = goal;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.todoTasks = todoTasks;
            this.inProgressTasks = inProgressTasks;
            this.inReviewTasks = inReviewTasks;
            this.progress = progress;
        }

        public UUID getSprintId() {
            return sprintId;
        }

        public void setSprintId(UUID sprintId) {
            this.sprintId = sprintId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGoal() {
            return goal;
        }

        public void setGoal(String goal) {
            this.goal = goal;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getTotalTasks() {
            return totalTasks;
        }

        public void setTotalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
        }

        public int getCompletedTasks() {
            return completedTasks;
        }

        public void setCompletedTasks(int completedTasks) {
            this.completedTasks = completedTasks;
        }

        public int getTodoTasks() {
            return todoTasks;
        }

        public void setTodoTasks(int todoTasks) {
            this.todoTasks = todoTasks;
        }

        public int getInProgressTasks() {
            return inProgressTasks;
        }

        public void setInProgressTasks(int inProgressTasks) {
            this.inProgressTasks = inProgressTasks;
        }

        public int getInReviewTasks() {
            return inReviewTasks;
        }

        public void setInReviewTasks(int inReviewTasks) {
            this.inReviewTasks = inReviewTasks;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }
    }

    public static class TaskMonitoringResponse {

        private UUID taskId;
        private String title;
        private String description;
        private String sprintName;
        private String assignedToName;
        private String priority;
        private String status;
        private int progress;

        public TaskMonitoringResponse() {
        }

        public TaskMonitoringResponse(
                UUID taskId,
                String title,
                String description,
                String sprintName,
                String assignedToName,
                String priority,
                String status,
                int progress) {

            this.taskId = taskId;
            this.title = title;
            this.description = description;
            this.sprintName = sprintName;
            this.assignedToName = assignedToName;
            this.priority = priority;
            this.status = status;
            this.progress = progress;
        }

        public UUID getTaskId() {
            return taskId;
        }

        public void setTaskId(UUID taskId) {
            this.taskId = taskId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSprintName() {
            return sprintName;
        }

        public void setSprintName(String sprintName) {
            this.sprintName = sprintName;
        }

        public String getAssignedToName() {
            return assignedToName;
        }

        public void setAssignedToName(String assignedToName) {
            this.assignedToName = assignedToName;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }
    }
}