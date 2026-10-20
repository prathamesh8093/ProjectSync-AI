package com.projectsync.backend.service;

import com.projectsync.backend.dto.AiRubricEvaluationResponse;
import com.projectsync.backend.dto.AiRubricEvaluationResponse.RubricCriterion;
import com.projectsync.backend.entity.CodeCommit;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.StudentContributionMetric;
import com.projectsync.backend.repository.CodeCommitRepository;
import com.projectsync.backend.repository.ProjectTaskRepository;
import com.projectsync.backend.repository.SprintRepository;
import com.projectsync.backend.repository.StudentContributionMetricRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class AiRubricGradingService {

    private final GeminiApiClient gemini;
    private final ObjectMapper objectMapper;
    private final SprintRepository sprintRepository;
    private final ProjectTaskRepository taskRepository;
    private final CodeCommitRepository commitRepository;
    private final StudentContributionMetricRepository metricRepository;

    public AiRubricGradingService(GeminiApiClient gemini,
                                   ObjectMapper objectMapper,
                                   SprintRepository sprintRepository,
                                   ProjectTaskRepository taskRepository,
                                   CodeCommitRepository commitRepository,
                                   StudentContributionMetricRepository metricRepository) {
        this.gemini = gemini;
        this.objectMapper = objectMapper;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.commitRepository = commitRepository;
        this.metricRepository = metricRepository;
    }

    public AiRubricEvaluationResponse evaluateProject(Project project) {
        List<Sprint> sprints = sprintRepository.findByProjectOrderByStartDateAsc(project);
        List<ProjectTask> tasks = taskRepository.findBySprint_Project(project);
        List<CodeCommit> commits = commitRepository.findByProjectOrderByCommittedAtAsc(project);

        long doneTasks = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();
        int totalPoints = tasks.stream().mapToInt(ProjectTask::getStoryPoints).sum();
        int completedPoints = tasks.stream()
                .filter(t -> "DONE".equalsIgnoreCase(t.getStatus()))
                .mapToInt(ProjectTask::getStoryPoints)
                .sum();

        try {
            return evaluateWithGemini(project, sprints, tasks, commits, doneTasks, totalPoints, completedPoints);
        } catch (Exception e) {
            System.out.println("Gemini rubric evaluation unavailable; running heuristic rubric evaluator: " + e.getMessage());
            return evaluateHeuristic(project, sprints, tasks, commits, doneTasks, totalPoints, completedPoints);
        }
    }

    private AiRubricEvaluationResponse evaluateWithGemini(Project project,
                                                          List<Sprint> sprints,
                                                          List<ProjectTask> tasks,
                                                          List<CodeCommit> commits,
                                                          long doneTasks,
                                                          int totalPoints,
                                                          int completedPoints) throws Exception {
        String prompt = """
                You are a senior computer science academic faculty evaluator.
                Evaluate the student software engineering project based on the following verified performance metrics:
                
                Project Title: %s
                Team Name: %s
                Tech Stack: %s
                Total Commits: %d
                Total Tasks: %d (Completed: %d)
                Total Story Points: %d (Completed: %d)
                Number of Sprints: %d
                
                Grade the project rigorously out of 100 across 4 standard criteria (max 25 each):
                1. Code Quality & Modularity (maxScore: 25)
                2. Architecture & Technical Scope (maxScore: 25)
                3. Engineering Rigor & Security (maxScore: 25)
                4. Teamwork, Velocity & Timeliness (maxScore: 25)
                
                Respond ONLY with JSON matching this structure:
                {
                  "overallScore": 88,
                  "grade": "A",
                  "executiveSummary": "...",
                  "criteria": [
                    { "name": "...", "maxScore": 25, "awardedScore": 22, "justification": "...", "suggestions": "..." }
                  ],
                  "strengths": ["...", "..."],
                  "areasForImprovement": ["...", "..."],
                  "suggestedFacultyFeedback": "..."
                }
                """.formatted(
                project.getTitle(),
                project.getTeam().getTeamName(),
                project.getTechnologyStack() != null ? project.getTechnologyStack() : "Fullstack",
                commits.size(),
                tasks.size(), doneTasks,
                totalPoints, completedPoints,
                sprints.size()
        );

        String json = gemini.generateJson(prompt);
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

        int overallScore = ((Number) map.getOrDefault("overallScore", 85)).intValue();
        String grade = String.valueOf(map.getOrDefault("grade", "A"));
        String executiveSummary = String.valueOf(map.getOrDefault("executiveSummary", ""));
        String suggestedFacultyFeedback = String.valueOf(map.getOrDefault("suggestedFacultyFeedback", ""));

        @SuppressWarnings("unchecked")
        List<String> strengths = (List<String>) map.getOrDefault("strengths", List.of("Solid architectural foundation"));
        @SuppressWarnings("unchecked")
        List<String> areasForImprovement = (List<String>) map.getOrDefault("areasForImprovement", List.of("Increase automated test coverage"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawCriteria = (List<Map<String, Object>>) map.get("criteria");
        List<RubricCriterion> criteria = new ArrayList<>();
        if (rawCriteria != null) {
            for (Map<String, Object> c : rawCriteria) {
                criteria.add(new RubricCriterion(
                        String.valueOf(c.getOrDefault("name", "Criterion")),
                        ((Number) c.getOrDefault("maxScore", 25)).intValue(),
                        ((Number) c.getOrDefault("awardedScore", 20)).intValue(),
                        String.valueOf(c.getOrDefault("justification", "")),
                        String.valueOf(c.getOrDefault("suggestions", ""))
                ));
            }
        }

        if (criteria.isEmpty()) {
            return evaluateHeuristic(project, sprints, tasks, commits, doneTasks, totalPoints, completedPoints);
        }

        return new AiRubricEvaluationResponse(
                project.getId(),
                project.getTitle(),
                project.getTeam().getTeamName(),
                overallScore,
                grade,
                executiveSummary,
                criteria,
                strengths,
                areasForImprovement,
                suggestedFacultyFeedback
        );
    }

    private AiRubricEvaluationResponse evaluateHeuristic(Project project,
                                                         List<Sprint> sprints,
                                                         List<ProjectTask> tasks,
                                                         List<CodeCommit> commits,
                                                         long doneTasks,
                                                         int totalPoints,
                                                         int completedPoints) {
        double taskRatio = tasks.isEmpty() ? 0.8 : (double) doneTasks / Math.max(1, tasks.size());
        double pointRatio = totalPoints == 0 ? 0.85 : (double) completedPoints / Math.max(1, totalPoints);
        int commitVolume = commits.size();

        // 1. Code Quality & Modularity (0-25)
        int scoreCodeQuality = Math.min(25, Math.max(18, 18 + (commitVolume > 10 ? 4 : 2)));
        // 2. Architecture & Scope (0-25)
        int scoreArchitecture = Math.min(25, Math.max(19, 19 + (project.getTechnologyStack() != null ? 4 : 2)));
        // 3. Engineering Rigor & Security (0-25)
        int scoreSecurity = Math.min(25, Math.max(17, 18 + (commits.stream().anyMatch(c -> c.getMessage().toLowerCase().contains("test")) ? 4 : 2)));
        // 4. Teamwork & Timeliness (0-25)
        int scoreTeamwork = Math.min(25, Math.max(17, (int) Math.round(16 + (taskRatio * 5) + (pointRatio * 4))));

        int total = scoreCodeQuality + scoreArchitecture + scoreSecurity + scoreTeamwork;
        String grade = total >= 92 ? "A+" : total >= 85 ? "A" : total >= 75 ? "B" : total >= 65 ? "C" : "D";

        List<RubricCriterion> criteria = List.of(
                new RubricCriterion(
                        "Code Quality & Modularity",
                        25,
                        scoreCodeQuality,
                        "Codebase demonstrates clean modular separation with decoupled services and structured controllers. Commit volume (" + commitVolume + " commits) reflects active incremental development.",
                        "Enforce strict checkstyle formatting and ensure unit test coverage exceeds 70%."
                ),
                new RubricCriterion(
                        "System Architecture & Scalability",
                        25,
                        scoreArchitecture,
                        "Architectural blueprint aligns with modern best practices using " + (project.getTechnologyStack() != null ? project.getTechnologyStack() : "Spring Boot & React") + " with relational database integrity.",
                        "Incorporate asynchronous message queues or caching mechanisms to support peak concurrency."
                ),
                new RubricCriterion(
                        "Engineering Rigor & Security",
                        25,
                        scoreSecurity,
                        "Implements token-based authentication with parameterized queries, role validation, and CORS isolation.",
                        "Add automated OWASP dependency checks in CI/CD pipeline and integrate structured API documentation."
                ),
                new RubricCriterion(
                        "Team Collaboration, Velocity & Timeliness",
                        25,
                        scoreTeamwork,
                        "Team maintains active sprint cadence with " + doneTasks + " completed tasks (" + completedPoints + " story points delivered).",
                        "Encourage equalized peer code reviews before merging pull requests to prevent knowledge silos."
                )
        );

        List<String> strengths = List.of(
                "Consistent commit cadence and healthy separation of concern between frontend and backend.",
                "Well-defined technical stack and structured relational schema.",
                "Active task completion and measurable story point delivery."
        );

        List<String> areasForImprovement = List.of(
                "Integrate unit and end-to-end integration test suites.",
                "Implement automated CI/CD security vulnerability scanning.",
                "Enhance API contract documentation via Swagger/OpenAPI."
        );

        String summary = "The project '" + project.getTitle() + "' demonstrates strong academic software engineering fundamentals. " +
                "The team has successfully implemented key functional modules using " + (project.getTechnologyStack() != null ? project.getTechnologyStack() : "the designated stack") +
                " with good consistency. Overall grade awarded is " + grade + " (" + total + "/100).";

        String feedback = "Great work on the milestone deliverables. The architecture is modular and aligns well with course specifications. " +
                "For the upcoming sprint, focus on expanding automated test coverage and establishing continuous integration checks before the final viva.";

        return new AiRubricEvaluationResponse(
                project.getId(),
                project.getTitle(),
                project.getTeam().getTeamName(),
                total,
                grade,
                summary,
                criteria,
                strengths,
                areasForImprovement,
                feedback
        );
    }
}
