package com.projectsync.backend.dto;

import java.util.List;
import java.util.UUID;

public record AiMilestoneRiskPredictionResponse(
        UUID projectId,
        String projectTitle,
        String teamName,
        String targetDeadline,
        String predictedCompletionDate,
        int delayDays,
        String delayRiskLevel,
        double currentVelocity,
        double requiredVelocity,
        double completionPercentage,
        int plannedPoints,
        int completedPoints,
        int remainingPoints,
        String aiRecommendation,
        List<String> riskFactors
) {}
