package com.projectsync.backend.dto;

import java.util.UUID;

public class FacultyProjectMonitoringResponse {

    private UUID projectId;
    private UUID proposalId;
    private UUID teamId;

    private String projectName;
    private String projectStatus;
    private String teamName;

    private int overallProgress;
    private int totalTasks;
    private int completedTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int inReviewTasks;

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
}