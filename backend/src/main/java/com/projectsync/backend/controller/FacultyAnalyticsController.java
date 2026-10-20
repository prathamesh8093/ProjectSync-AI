package com.projectsync.backend.controller;

import com.projectsync.backend.entity.AiProjectInsight;
import com.projectsync.backend.entity.CodeCommit;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.AiProjectInsightRepository;
import com.projectsync.backend.repository.CodeCommitRepository;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.ProjectTaskRepository;
import com.projectsync.backend.repository.SprintRepository;
import com.projectsync.backend.repository.StudentContributionMetricRepository;
import com.projectsync.backend.repository.UserRepository;
import com.projectsync.backend.service.AiCodeInspectorService;
import com.projectsync.backend.service.AiCodeQualityInspectorService;
import com.projectsync.backend.service.AiMilestoneRiskPredictorService;
import com.projectsync.backend.service.AiRubricGradingService;
import com.projectsync.backend.service.ContributionAnalyticsService;
import com.projectsync.backend.service.EvaluationExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/faculty/analytics/projects")
public class FacultyAnalyticsController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final ProjectTaskRepository taskRepository;
    private final CodeCommitRepository commitRepository;
    private final StudentContributionMetricRepository metricRepository;
    private final AiProjectInsightRepository insightRepository;
    private final AiCodeInspectorService inspector;
    private final EvaluationExportService exportService;
    private final AiRubricGradingService rubricGradingService;
    private final AiCodeQualityInspectorService codeQualityInspectorService;
    private final AiMilestoneRiskPredictorService riskPredictorService;

    public FacultyAnalyticsController(UserRepository userRepository,
                                      ProjectRepository projectRepository,
                                      SprintRepository sprintRepository,
                                      ProjectTaskRepository taskRepository,
                                      CodeCommitRepository commitRepository,
                                      StudentContributionMetricRepository metricRepository,
                                      AiProjectInsightRepository insightRepository,
                                      AiCodeInspectorService inspector,
                                      EvaluationExportService exportService,
                                      AiRubricGradingService rubricGradingService,
                                      AiCodeQualityInspectorService codeQualityInspectorService,
                                      AiMilestoneRiskPredictorService riskPredictorService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.commitRepository = commitRepository;
        this.metricRepository = metricRepository;
        this.insightRepository = insightRepository;
        this.inspector = inspector;
        this.exportService = exportService;
        this.rubricGradingService = rubricGradingService;
        this.codeQualityInspectorService = codeQualityInspectorService;
        this.riskPredictorService = riskPredictorService;
    }

    @GetMapping
    public ResponseEntity<?> listProjects(Authentication authentication) {
        if (!isFaculty(authentication)) return forbidden();
        return ResponseEntity.ok(projectRepository.findByStatusIn(List.of("APPROVED", "ACTIVE")).stream()
                .map(p -> Map.of("id", p.getId(), "title", p.getTitle(), "teamName", p.getTeam().getTeamName()))
                .toList());
    }

    @GetMapping("/{projectId}/analytics")
    public ResponseEntity<?> getAnalytics(Authentication authentication, @PathVariable UUID projectId,
                                          @RequestParam(required = false) UUID sprintId) {
        if (!isFaculty(authentication)) return forbidden();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found.");
        return ResponseEntity.ok(buildDashboard(project, sprintId));
    }

    @PostMapping("/{projectId}/sprints/{sprintId}/analyze")
    public ResponseEntity<?> analyzeSprint(Authentication authentication, @PathVariable UUID projectId,
                                           @PathVariable UUID sprintId) {
        if (!isFaculty(authentication)) return forbidden();
        Sprint sprint = sprintRepository.findById(sprintId).orElse(null);
        if (sprint == null || !sprint.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sprint not found.");
        }
        inspector.analyzeSprint(sprint);
        return ResponseEntity.ok(buildDashboard(sprint.getProject(), sprintId));
    }

    @GetMapping(value = "/{projectId}/export-report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> exportReport(Authentication authentication, @PathVariable UUID projectId) {
        if (!isFaculty(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only faculty can access this resource.");
        return projectRepository.findById(projectId)
                .map(p -> ResponseEntity.ok(exportService.buildReport(p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found."));
    }

    @GetMapping("/{projectId}/rubric-evaluation")
    public ResponseEntity<?> evaluateRubric(Authentication authentication, @PathVariable UUID projectId) {
        if (!isFaculty(authentication)) return forbidden();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found.");
        return ResponseEntity.ok(rubricGradingService.evaluateProject(project));
    }

    @GetMapping("/{projectId}/code-inspection")
    public ResponseEntity<?> inspectCodeQuality(Authentication authentication, @PathVariable UUID projectId) {
        if (!isFaculty(authentication)) return forbidden();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found.");
        return ResponseEntity.ok(codeQualityInspectorService.inspectProject(project));
    }

    @GetMapping("/{projectId}/delay-risk-prediction")
    public ResponseEntity<?> predictDelayRisk(Authentication authentication, @PathVariable UUID projectId) {
        if (!isFaculty(authentication)) return forbidden();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found.");
        return ResponseEntity.ok(riskPredictorService.predictMilestoneRisk(project));
    }


    private Map<String, Object> buildDashboard(Project project, UUID requestedSprintId) {
        List<Sprint> sprints = sprintRepository.findByProjectOrderByStartDateAsc(project);
        List<ProjectTask> tasks = taskRepository.findBySprint_Project(project);
        List<CodeCommit> commits = commitRepository.findByProjectOrderByCommittedAtAsc(project);

        List<Map<String, Object>> timeline = sprints.stream().map(s -> {
            List<CodeCommit> inSprint = commits.stream().filter(c -> ContributionAnalyticsService.inSprint(c, s)).toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("name", s.getName());
            row.put("startDate", s.getStartDate());
            row.put("endDate", s.getEndDate());
            row.put("plannedPoints", s.getPlannedPoints());
            row.put("completedPoints", tasks.stream()
                    .filter(t -> t.getSprint().getId().equals(s.getId()) && "DONE".equals(t.getStatus()))
                    .mapToInt(ProjectTask::getStoryPoints).sum());
            row.put("commits", inSprint.size());
            row.put("codeVolume", inSprint.stream().mapToDouble(c -> c.getLinesAdded() + 0.5 * c.getLinesDeleted()).sum());
            return row;
        }).toList();

        // Default to the latest sprint that has started.
        Sprint selected = sprints.stream().filter(s -> s.getId().equals(requestedSprintId)).findFirst()
                .orElseGet(() -> sprints.stream().filter(s -> !s.getStartDate().isAfter(LocalDate.now()))
                        .reduce((a, b) -> b).orElse(sprints.isEmpty() ? null : sprints.get(0)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", Map.of("id", project.getId(), "title", project.getTitle(),
                "teamName", project.getTeam().getTeamName(), "status", project.getStatus()));
        result.put("sprints", timeline);
        result.put("selectedSprintId", selected == null ? null : selected.getId());

        if (selected != null) {
            result.put("metrics", metricRepository.findBySprint(selected).stream().map(m -> Map.of(
                    "studentUserId", m.getStudent().getId(),
                    "fullName", m.getStudent().getFullName(),
                    "totalCommits", m.getTotalCommits(),
                    "linesAdded", m.getLinesAdded(),
                    "linesDeleted", m.getLinesDeleted(),
                    "tasksCompleted", m.getTasksCompleted(),
                    "consistencyScore", m.getConsistencyScore(),
                    "contributionPercentage", m.getContributionPercentage())).toList());
            AiProjectInsight insight = insightRepository.findFirstBySprintOrderByGeneratedAtDesc(selected).orElse(null);
            if (insight != null) {
                Map<String, Object> i = new LinkedHashMap<>();
                i.put("riskLevel", insight.getRiskLevel());
                i.put("executiveSummary", insight.getExecutiveSummary());
                i.put("flags", inspector.parseFlags(insight));
                i.put("generatedAt", insight.getGeneratedAt());
                result.put("insight", i);
            }
        }
        return result;
    }

    private boolean isFaculty(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .map(User::getRole).filter("FACULTY"::equals).isPresent();
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only faculty can access this resource.");
    }
}
