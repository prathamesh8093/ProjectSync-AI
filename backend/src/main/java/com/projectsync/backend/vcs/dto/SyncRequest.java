package com.projectsync.backend.vcs.dto;

public class SyncRequest {

    private String remoteName = "origin";
    private String branch;
    private String username;
    private String personalAccessToken; // in-memory only, never persisted
    private boolean force;

    public SyncRequest() {
    }

    public SyncRequest(String remoteName, String branch, String username, String personalAccessToken, boolean force) {
        this.remoteName = remoteName != null && !remoteName.isBlank() ? remoteName : "origin";
        this.branch = branch;
        this.username = username;
        this.personalAccessToken = personalAccessToken;
        this.force = force;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
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

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
