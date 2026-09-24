package com.projectsync.backend.vcs.model;

import java.time.Instant;

/**
 * Metadata model for a Git repository managed by ProjectSync VCS.
 * Stored locally in JSON format or in Supabase when available.
 */
public class RepositoryMetadata {

    private String id;
    private String name;
    private String description;
    private String diskPath;
    private String defaultBranch;
    private String remoteUrl;
    private String ownerUsername;
    private Instant createdAt;
    private Instant updatedAt;

    public RepositoryMetadata() {
    }

    public RepositoryMetadata(
            String id,
            String name,
            String description,
            String diskPath,
            String defaultBranch,
            String remoteUrl,
            String ownerUsername,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.diskPath = diskPath;
        this.defaultBranch = defaultBranch;
        this.remoteUrl = remoteUrl;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
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
}
