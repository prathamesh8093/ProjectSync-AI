package com.projectsync.backend.vcs.dto;

import jakarta.validation.constraints.NotBlank;

public class CommitRequest {

    @NotBlank(message = "Commit message is required")
    private String message;

    private String authorName;

    private String authorEmail;

    public CommitRequest() {
    }

    public CommitRequest(String message, String authorName, String authorEmail) {
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }
}
