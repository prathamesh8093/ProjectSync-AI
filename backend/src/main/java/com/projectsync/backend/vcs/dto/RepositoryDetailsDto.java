package com.projectsync.backend.vcs.dto;

import java.time.Instant;
import java.util.List;

public class RepositoryDetailsDto {

    private String id;
    private String name;
    private String description;
    private String diskPath;
    private String defaultBranch;
    private String currentBranch;
    private boolean isClean;
    private int commitCount;
    private String remoteUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private List<RemoteConfigDto> remotes;

    public RepositoryDetailsDto() {
    }

    public RepositoryDetailsDto(
            String id,
            String name,
            String description,
            String diskPath,
            String defaultBranch,
            String currentBranch,
            boolean isClean,
            int commitCount,
            String remoteUrl,
            Instant createdAt,
            Instant updatedAt,
            List<RemoteConfigDto> remotes) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.diskPath = diskPath;
        this.defaultBranch = defaultBranch;
        this.currentBranch = currentBranch;
        this.isClean = isClean;
        this.commitCount = commitCount;
        this.remoteUrl = remoteUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.remotes = remotes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDiskPath() {
        return diskPath;
    }

    public void setDiskPath(String diskPath) {
        this.diskPath = diskPath;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    public boolean isClean() {
        return isClean;
    }

    public void setClean(boolean clean) {
        isClean = clean;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RemoteConfigDto> getRemotes() {
        return remotes;
    }

    public void setRemotes(List<RemoteConfigDto> remotes) {
        this.remotes = remotes;
    }
}
