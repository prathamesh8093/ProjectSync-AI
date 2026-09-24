package com.projectsync.backend.vcs.dto;

public class BranchDto {

    private String name;
    private boolean isCurrent;
    private String headCommitSha;
    private String headCommitMessage;

    public BranchDto() {
    }

    public BranchDto(String name, boolean isCurrent, String headCommitSha, String headCommitMessage) {
        this.name = name;
        this.isCurrent = isCurrent;
        this.headCommitSha = headCommitSha;
        this.headCommitMessage = headCommitMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public String getHeadCommitSha() {
        return headCommitSha;
    }

    public void setHeadCommitSha(String headCommitSha) {
        this.headCommitSha = headCommitSha;
    }

    public String getHeadCommitMessage() {
        return headCommitMessage;
    }

    public void setHeadCommitMessage(String headCommitMessage) {
        this.headCommitMessage = headCommitMessage;
    }
}
