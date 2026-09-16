package com.projectsync.backend.dto;

import java.util.UUID;

public class ProjectProgressResponse {
    private UUID projectId;
    private String projectName;
    private String projectStatus;
    private int overallProgress;
    private int totalTasks;
    private int completedTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int inReviewTasks;

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }
    public int getOverallProgress() { return overallProgress; }
    public void setOverallProgress(int overallProgress) { this.overallProgress = overallProgress; }
    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
    public int getTodoTasks() { return todoTasks; }
    public void setTodoTasks(int todoTasks) { this.todoTasks = todoTasks; }
    public int getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(int inProgressTasks) { this.inProgressTasks = inProgressTasks; }
    public int getInReviewTasks() { return inReviewTasks; }
    public void setInReviewTasks(int inReviewTasks) { this.inReviewTasks = inReviewTasks; }
}
