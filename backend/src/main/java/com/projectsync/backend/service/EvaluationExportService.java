package com.projectsync.backend.service;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.service.ContributionAnalyticsService.StudentStats;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/** Printable viva evaluation sheet (HTML; faculty prints or saves as PDF from the browser). */
@Service
public class EvaluationExportService {

    private final ContributionAnalyticsService analytics;
    private final TeamMemberRepository teamMemberRepository;

    public EvaluationExportService(ContributionAnalyticsService analytics, TeamMemberRepository teamMemberRepository) {
        this.analytics = analytics;
        this.teamMemberRepository = teamMemberRepository;
    }

    /**
     * Grade from the student's share relative to an equal split.
     * ponytail: share-only heuristic; fold in consistency/flags if faculty want a stricter rubric.
     */
    static String suggestedGrade(StudentStats s, int teamSize) {
        if (s.commits() == 0) return "Incomplete";
        double ratio = s.share() * teamSize; // 1.0 = exactly a fair share
        if (ratio >= 0.75) return "A";
        if (ratio >= 0.4) return "B";
        return "C";
    }

    public String buildReport(Project project) {
        List<StudentStats> stats = analytics.forProject(project);
        Map<UUID, String> roles = teamMemberRepository.findByTeam(project.getTeam()).stream()
                .collect(Collectors.toMap(m -> m.getStudentUser().getId(), TeamMember::getTeamRole));

        String rows = stats.stream().map(s -> """
                <tr>
                  <td>%s</td><td>%s</td><td>%d</td><td>%d</td><td>%.1f%%</td><td><strong>%s</strong></td><td></td>
                </tr>""".formatted(
                htmlEscape(s.student().getFullName()),
                htmlEscape(roles.getOrDefault(s.student().getId(), "")),
                s.tasksCompleted(), s.commits(), s.share() * 100,
                suggestedGrade(s, stats.size()))).collect(Collectors.joining("\n"));

        return """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8"><title>Viva Evaluation – %s</title>
                <style>
                  body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; color: #1A1D29; margin: 40px; }
                  h1 { font-size: 20px; margin: 0 0 4px; } p.meta { color: #5B6072; margin: 0 0 24px; font-size: 13px; }
                  table { width: 100%%; border-collapse: collapse; font-size: 13px; }
                  th, td { border: 1px solid #C9CDD6; padding: 10px; text-align: left; }
                  th { background: #F1F3F6; } td:last-child { width: 120px; }
                  .note { font-size: 12px; color: #5B6072; margin-top: 12px; }
                  .sign { margin-top: 64px; display: flex; gap: 64px; font-size: 13px; }
                  .sign div { border-top: 1px solid #1A1D29; padding-top: 6px; width: 240px; }
                  @media print { body { margin: 16mm; } }
                </style></head>
                <body>
                  <h1>Viva Evaluation Sheet</h1>
                  <p class="meta">Project: <strong>%s</strong> · Team: %s · Generated %s</p>
                  <table>
                    <thead><tr>
                      <th>Student</th><th>Team Role</th><th>Tasks Completed</th><th>Commits</th>
                      <th>Verified Code Contribution</th><th>AI Suggested Grade</th><th>Rubric Marks</th>
                    </tr></thead>
                    <tbody>%s</tbody>
                  </table>
                  <p class="note">Code contribution = lines added + 0.5 × lines deleted, as a share of the team total.
                  The suggested grade is advisory only.</p>
                  <div class="sign"><div>Faculty Signature</div><div>Date</div></div>
                </body></html>
                """.formatted(
                htmlEscape(project.getTitle()), htmlEscape(project.getTitle()),
                htmlEscape(project.getTeam().getTeamName()), LocalDate.now(), rows);
    }
}
