package com.projectsync.backend.vcs.dto;

public class CommitFileDiffDto {

    private String changeType; // ADD, MODIFY, DELETE, RENAME, COPY
    private String oldPath;
    private String newPath;
    private int additions;
    private int deletions;
    private String patch;

    public CommitFileDiffDto() {
    }

    public CommitFileDiffDto(String changeType, String oldPath, String newPath, int additions, int deletions, String patch) {
        this.changeType = changeType;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.additions = additions;
        this.deletions = deletions;
        this.patch = patch;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getOldPath() {
        return oldPath;
    }

    public void setOldPath(String oldPath) {
        this.oldPath = oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public int getAdditions() {
        return additions;
    }

    public void setAdditions(int additions) {
        this.additions = additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }
}
