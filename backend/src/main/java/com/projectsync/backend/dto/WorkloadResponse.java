package com.projectsync.backend.dto;

import java.util.UUID;

public class WorkloadResponse {

    private UUID userId;
    private String fullName;
    private int totalTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int inReviewTasks;
    private int completedTasks;
    private int averageProgress;

    public WorkloadResponse() {
    }

    public WorkloadResponse(
            UUID userId,
            String fullName,
            int totalTasks,
            int todoTasks,
            int inProgressTasks,
            int inReviewTasks,
            int completedTasks,
            int averageProgress) {

        this.userId = userId;
        this.fullName = fullName;
        this.totalTasks = totalTasks;
        this.todoTasks = todoTasks;
        this.inProgressTasks = inProgressTasks;
        this.inReviewTasks = inReviewTasks;
        this.completedTasks = completedTasks;
        this.averageProgress = averageProgress;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
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

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getAverageProgress() {
        return averageProgress;
    }

    public void setAverageProgress(int averageProgress) {
        this.averageProgress = averageProgress;
    }
}