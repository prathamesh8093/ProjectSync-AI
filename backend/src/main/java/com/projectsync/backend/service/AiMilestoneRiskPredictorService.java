package com.projectsync.backend.service;

import com.projectsync.backend.dto.AiMilestoneRiskPredictionResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.repository.ProjectTaskRepository;
import com.projectsync.backend.repository.SprintRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiMilestoneRiskPredictorService {

    private final SprintRepository sprintRepository;
    private final ProjectTaskRepository taskRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public AiMilestoneRiskPredictorService(SprintRepository sprintRepository,
                                           ProjectTaskRepository taskRepository) {
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
    }

    public AiMilestoneRiskPredictionResponse predictMilestoneRisk(Project project) {
        List<Sprint> sprints = sprintRepository.findByProjectOrderByStartDateAsc(project);
        List<ProjectTask> tasks = taskRepository.findBySprint_Project(project);

        int totalPlanned = tasks.stream().mapToInt(ProjectTask::getStoryPoints).sum();
        int completedPoints = tasks.stream()
                .filter(t -> "DONE".equalsIgnoreCase(t.getStatus()))
                .mapToInt(ProjectTask::getStoryPoints)
                .sum();

        if (totalPlanned == 0) {
            totalPlanned = sprints.stream().mapToInt(Sprint::getPlannedPoints).sum();
            if (totalPlanned == 0) totalPlanned = 40; // baseline default
        }

        int remainingPoints = Math.max(0, totalPlanned - completedPoints);
        double completionPercentage = totalPlanned > 0
                ? ((double) completedPoints / totalPlanned) * 100.0
                : 0.0;

        LocalDate targetDeadline = sprints.isEmpty()
                ? LocalDate.now().plusWeeks(6)
                : sprints.get(sprints.size() - 1).getEndDate();

        // Calculate velocity (approx points / week)
        double currentVelocity = completedPoints > 0 ? (double) completedPoints / 2.5 : 8.5;
        double weeksLeft = Math.max(1, (targetDeadline.toEpochDay() - LocalDate.now().toEpochDay()) / 7.0);
        double requiredVelocity = remainingPoints / weeksLeft;

        int delayDays = 0;
        String riskLevel = "LOW";
        if (requiredVelocity > currentVelocity * 1.3) {
            delayDays = (int) Math.round(((remainingPoints / Math.max(currentVelocity, 1.0)) - weeksLeft) * 7);
            if (delayDays > 7) {
                riskLevel = "HIGH";
            } else if (delayDays > 2) {
                riskLevel = "MEDIUM";
            }
        }

        LocalDate predictedDate = targetDeadline.plusDays(delayDays);

        List<String> riskFactors = new ArrayList<>();
        if (delayDays > 0) {
            riskFactors.add("Required velocity (" + String.format("%.1f", requiredVelocity) + " pts/wk) exceeds current pace (" + String.format("%.1f", currentVelocity) + " pts/wk).");
        } else {
            riskFactors.add("Current burn-down trajectory is stable and tracking on schedule.");
        }
        if (remainingPoints > 20) {
            riskFactors.add("High volume of unfinished story points (" + remainingPoints + " pts) slated for final modules.");
        }

        String recommendation;
        if ("HIGH".equals(riskLevel)) {
            recommendation = "High risk of milestone overrun (+ " + delayDays + " days). Faculty intervention recommended: schedule an immediate sprint backlog grooming session to de-scope optional stretch features.";
        } else if ("MEDIUM".equals(riskLevel)) {
            recommendation = "Moderate pace variance (+ " + delayDays + " days). Recommend pair-programming on complex backend services and daily standup check-ins.";
        } else {
            recommendation = "Milestone timeline is healthy and tracking on schedule. Expected completion by " + predictedDate.format(DATE_FMT) + ".";
        }

        return new AiMilestoneRiskPredictionResponse(
                project.getId(),
                project.getTitle(),
                project.getTeam().getTeamName(),
                targetDeadline.format(DATE_FMT),
                predictedDate.format(DATE_FMT),
                delayDays,
                riskLevel,
                Math.round(currentVelocity * 10.0) / 10.0,
                Math.round(requiredVelocity * 10.0) / 10.0,
                Math.round(completionPercentage * 10.0) / 10.0,
                totalPlanned,
                completedPoints,
                remainingPoints,
                recommendation,
                riskFactors
        );
    }
}
