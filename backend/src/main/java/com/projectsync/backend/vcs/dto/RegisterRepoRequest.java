package com.projectsync.backend.vcs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterRepoRequest {

    @NotBlank(message = "Repository name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Repository name can only contain letters, numbers, underscores, hyphens, and periods")
    private String name;

    @NotBlank(message = "Local directory path is required")
    private String path;

    private String description;

    public RegisterRepoRequest() {
    }

    public RegisterRepoRequest(String name, String path, String description) {
        this.name = name;
        this.path = path;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
