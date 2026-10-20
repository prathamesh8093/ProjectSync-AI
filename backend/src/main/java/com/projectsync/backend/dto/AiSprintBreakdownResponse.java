package com.projectsync.backend.dto;

import java.util.List;
import java.util.UUID;

public record AiSprintBreakdownResponse(
        UUID projectId,
        String projectTitle,
        int totalStoryPoints,
        List<SprintBreakdown> sprints
) {
    public record SprintBreakdown(
            int sprintNumber,
            String sprintName,
            String sprintGoal,
            int plannedPoints,
            List<UserStory> userStories
    ) {}

    public record UserStory(
            String storyId,
            String title,
            String userStoryText,
            List<String> acceptanceCriteria,
            int storyPoints,
            String recommendedRole
    ) {}
}
