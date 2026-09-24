package com.projectsync.backend.vcs.dto;

public class DiffDto {

    private String filePath;
    private boolean staged;
    private String patch;
    private int additions;
    private int deletions;

    public DiffDto() {
    }

    public DiffDto(String filePath, boolean staged, String patch, int additions, int deletions) {
        this.filePath = filePath;
        this.staged = staged;
        this.patch = patch;
        this.additions = additions;
        this.deletions = deletions;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isStaged() {
        return staged;
    }

    public void setStaged(boolean staged) {
        this.staged = staged;
    }

    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
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
}
