package com.projectsync.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentAssessmentRequest(
        @NotNull @Min(1) @Max(10) Integer frontendScore,
        @NotNull @Min(1) @Max(10) Integer backendScore,
        @NotNull @Min(1) @Max(10) Integer databaseScore,
        @NotNull @Min(1) @Max(10) Integer devopsScore,
        @NotNull @Min(1) @Max(10) Integer mlAiScore,
        @Size(max = 50) String primaryInterest,
        @Size(max = 2000) String pastProjects) {
}
