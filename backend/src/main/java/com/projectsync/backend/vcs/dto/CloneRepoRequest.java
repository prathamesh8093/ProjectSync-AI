package com.projectsync.backend.vcs.dto;

import jakarta.validation.constraints.NotBlank;

public class CloneRepoRequest {

    @NotBlank(message = "Remote repository URL cannot be empty")
    private String remoteUrl;

    private String name;
    private String description;
    private String branch;
    private String username;
    private String personalAccessToken;

    public CloneRepoRequest() {}

    public CloneRepoRequest(String remoteUrl, String name, String description, String branch, String username, String personalAccessToken) {
        this.remoteUrl = remoteUrl;
        this.name = name;
        this.description = description;
        this.branch = branch;
        this.username = username;
        this.personalAccessToken = personalAccessToken;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
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

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPersonalAccessToken() {
        return personalAccessToken;
    }

    public void setPersonalAccessToken(String personalAccessToken) {
        this.personalAccessToken = personalAccessToken;
    }
}
