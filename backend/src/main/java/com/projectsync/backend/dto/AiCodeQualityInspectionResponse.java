package com.projectsync.backend.dto;

import java.util.List;
import java.util.UUID;

public record AiCodeQualityInspectionResponse(
        UUID projectId,
        String projectTitle,
        String teamName,
        String overallCodeGrade,
        int overallQualityScore,
        String maintainabilityIndex,
        String testCoverageEstimate,
        List<StudentRoleAlignment> roleAlignments,
        List<VulnerabilityItem> securityVulnerabilities,
        List<String> codeSmells,
        String executiveAuditSummary
) {
    public record StudentRoleAlignment(
            String studentName,
            String assignedRole,
            int alignmentPercentage,
            String status,
            String details
    ) {}

    public record VulnerabilityItem(
            String title,
            String severity,
            String location,
            String recommendation
    ) {}
}
