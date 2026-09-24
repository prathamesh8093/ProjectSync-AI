package com.projectsync.backend.vcs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateRepoRequest {

    @NotBlank(message = "Repository name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Repository name can only contain letters, numbers, underscores, hyphens, and periods")
    private String name;

    private String description;

    private String defaultBranch = "main";

    private boolean initReadme = true;

    public CreateRepoRequest() {
    }

    public CreateRepoRequest(String name, String description, String defaultBranch, boolean initReadme) {
        this.name = name;
        this.description = description;
        this.defaultBranch = defaultBranch != null && !defaultBranch.isBlank() ? defaultBranch : "main";
        this.initReadme = initReadme;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public boolean isInitReadme() {
        return initReadme;
    }

    public void setInitReadme(boolean initReadme) {
        this.initReadme = initReadme;
    }
}
