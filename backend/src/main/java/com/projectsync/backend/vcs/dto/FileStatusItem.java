package com.projectsync.backend.vcs.dto;

public class FileStatusItem {

    private String filePath;
    private String status; // ADDED, MODIFIED, DELETED, RENAMED, UNTRACKED, CONFLICT

    public FileStatusItem() {
    }

    public FileStatusItem(String filePath, String status) {
        this.filePath = filePath;
        this.status = status;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
