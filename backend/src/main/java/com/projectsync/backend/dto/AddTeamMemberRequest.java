package com.projectsync.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddTeamMemberRequest {

    @NotNull
    private UUID studentUserId;

    public UUID getStudentUserId() { return studentUserId; }
    public void setStudentUserId(UUID studentUserId) { this.studentUserId = studentUserId; }
}