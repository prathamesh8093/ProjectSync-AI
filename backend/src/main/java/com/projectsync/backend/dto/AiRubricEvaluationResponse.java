package com.projectsync.backend.dto;

import java.util.List;
import java.util.UUID;

public record AiRubricEvaluationResponse(
        UUID projectId,
        String projectTitle,
        String teamName,
        int overallScore,
        String grade,
        String executiveSummary,
        List<RubricCriterion> criteria,
        List<String> strengths,
        List<String> areasForImprovement,
        String suggestedFacultyFeedback
) {
    public record RubricCriterion(
            String name,
            int maxScore,
            int awardedScore,
            String justification,
            String suggestions
    ) {}
}
