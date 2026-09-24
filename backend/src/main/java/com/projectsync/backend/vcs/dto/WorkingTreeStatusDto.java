package com.projectsync.backend.vcs.dto;

import java.util.ArrayList;
import java.util.List;

public class WorkingTreeStatusDto {

    private String repoId;
    private String currentBranch;
    private boolean isClean;
    private List<FileStatusItem> staged = new ArrayList<>();
    private List<FileStatusItem> unstaged = new ArrayList<>();
    private List<String> untracked = new ArrayList<>();
    private List<String> conflicting = new ArrayList<>();

    public WorkingTreeStatusDto() {
    }

    public WorkingTreeStatusDto(
            String repoId,
            String currentBranch,
            boolean isClean,
            List<FileStatusItem> staged,
            List<FileStatusItem> unstaged,
            List<String> untracked,
            List<String> conflicting) {
        this.repoId = repoId;
        this.currentBranch = currentBranch;
        this.isClean = isClean;
        this.staged = staged != null ? staged : new ArrayList<>();
        this.unstaged = unstaged != null ? unstaged : new ArrayList<>();
        this.untracked = untracked != null ? untracked : new ArrayList<>();
        this.conflicting = conflicting != null ? conflicting : new ArrayList<>();
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
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

    public List<FileStatusItem> getStaged() {
        return staged;
    }

    public void setStaged(List<FileStatusItem> staged) {
        this.staged = staged;
    }

    public List<FileStatusItem> getUnstaged() {
        return unstaged;
    }

    public void setUnstaged(List<FileStatusItem> unstaged) {
        this.unstaged = unstaged;
    }

    public List<String> getUntracked() {
        return untracked;
    }

    public void setUntracked(List<String> untracked) {
        this.untracked = untracked;
    }

    public List<String> getConflicting() {
        return conflicting;
    }

    public void setConflicting(List<String> conflicting) {
        this.conflicting = conflicting;
    }
}
