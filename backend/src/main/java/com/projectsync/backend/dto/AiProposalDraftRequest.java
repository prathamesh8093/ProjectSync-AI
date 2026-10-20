package com.projectsync.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AiProposalDraftRequest(
        @NotBlank(message = "Project idea is required")
        String idea,

        String technologyStack,

        Integer durationWeeks
) {}
