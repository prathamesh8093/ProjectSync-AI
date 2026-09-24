package com.projectsync.backend.vcs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateBranchRequest {

    @NotBlank(message = "Branch name is required")
    @Pattern(regexp = "^(?!/|.*([/.]\\.|//|@\\{|\\\\))[^\\x00-\\x20 ~^:?*\\[]+(?<!/)$", message = "Invalid Git branch name")
    private String branchName;

    private String startPoint; // optional commit SHA or source branch name

    public CreateBranchRequest() {
    }

    public CreateBranchRequest(String branchName, String startPoint) {
        this.branchName = branchName;
        this.startPoint = startPoint;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }
}
