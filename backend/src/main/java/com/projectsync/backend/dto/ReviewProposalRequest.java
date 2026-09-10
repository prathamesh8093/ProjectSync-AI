package com.projectsync.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ReviewProposalRequest {

    @NotBlank
    private String decision;

    private String comments;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}