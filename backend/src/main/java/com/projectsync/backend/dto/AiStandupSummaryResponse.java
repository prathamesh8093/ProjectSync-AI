package com.projectsync.backend.dto;

import java.util.List;

public record AiStandupSummaryResponse(
        String teamName,
        String summaryDate,
        String executiveSummary,
        List<String> accomplishments,
        List<String> plannedNextSteps,
        List<String> blockers,
        List<ActionItem> actionItems
) {
    public record ActionItem(
            String task,
            String assignee,
            String priority
    ) {}
}
