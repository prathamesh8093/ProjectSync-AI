package com.projectsync.backend.vcs.dto;

import java.time.Instant;

public class RepositorySummaryDto {

    private String id;
    private String name;
    private String description;
    private String diskPath;
    private String currentBranch;
    private int commitCount;
    private boolean isClean;
    private Instant lastCommitDate;
    private String remoteUrl;

    public RepositorySummaryDto() {
    }

    public RepositorySummaryDto(
            String id,
            String name,
            String description,
            String diskPath,
            String currentBranch,
            int commitCount,
            boolean isClean,
            Instant lastCommitDate,
            String remoteUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.diskPath = diskPath;
        this.currentBranch = currentBranch;
        this.commitCount = commitCount;
        this.isClean = isClean;
        this.lastCommitDate = lastCommitDate;
        this.remoteUrl = remoteUrl;
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

    public String getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public boolean isClean() {
        return isClean;
    }

    public void setClean(boolean clean) {
        isClean = clean;
    }

    public Instant getLastCommitDate() {
        return lastCommitDate;
    }

    public void setLastCommitDate(Instant lastCommitDate) {
        this.lastCommitDate = lastCommitDate;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
}
