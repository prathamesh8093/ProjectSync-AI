package com.projectsync.backend.dto;

import java.util.List;

public record AiProposalDraftResponse(
        String title,
        String abstractText,
        String objectives,
        String literatureReview,
        String researchGaps,
        String technologyStack,
        String futureScope,
        List<ModuleTimeline> modules,
        String formattedDescription,
        String formattedObjectives
) {
    public record ModuleTimeline(
            String moduleNumber,
            String moduleName,
            String startDate,
            String endDate,
            String deliverables
    ) {}
}
