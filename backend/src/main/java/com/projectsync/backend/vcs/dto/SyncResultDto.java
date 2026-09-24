package com.projectsync.backend.vcs.dto;

public class SyncResultDto {

    private boolean success;
    private String operation; // FETCH, PULL, PUSH
    private String message;
    private String details;

    public SyncResultDto() {
    }

    public SyncResultDto(boolean success, String operation, String message, String details) {
        this.success = success;
        this.operation = operation;
        this.message = message;
        this.details = details;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
