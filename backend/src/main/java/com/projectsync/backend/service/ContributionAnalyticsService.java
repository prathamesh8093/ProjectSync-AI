package com.projectsync.backend.service;

import com.projectsync.backend.entity.CodeCommit;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.StudentContributionMetric;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.CodeCommitRepository;
import com.projectsync.backend.repository.ProjectTaskRepository;
import com.projectsync.backend.repository.StudentContributionMetricRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns raw commits + Kanban tasks into per-student effort metrics. */
@Service
public class ContributionAnalyticsService {

    public record StudentStats(User student, int commits, int linesAdded, int linesDeleted,
                               int tasksAssigned, int tasksCompleted, int pointsCompleted,
                               double volume, double consistency, double share) {}

    private final CodeCommitRepository commitRepository;
    private final ProjectTaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final StudentContributionMetricRepository metricRepository;

    public ContributionAnalyticsService(CodeCommitRepository commitRepository,
                                        ProjectTaskRepository taskRepository,
                                        TeamMemberRepository teamMemberRepository,
                                        StudentContributionMetricRepository metricRepository) {
        this.commitRepository = commitRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.metricRepository = metricRepository;
    }

    public List<User> students(Project project) {
        return teamMemberRepository.findByTeam(project.getTeam()).stream()
                .map(TeamMember::getStudentUser)
                .toList();
    }

    public static OffsetDateTime sprintStart(Sprint sprint) {
        return sprint.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    /** Exclusive end: midnight after the sprint's last day. */
    public static OffsetDateTime sprintEnd(Sprint sprint) {
        return sprint.getEndDate().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    public static boolean inSprint(CodeCommit c, Sprint sprint) {
        return !c.getCommittedAt().isBefore(sprintStart(sprint)) && c.getCommittedAt().isBefore(sprintEnd(sprint));
    }

    public List<CodeCommit> sprintCommits(Sprint sprint) {
        return commitRepository.findByProjectOrderByCommittedAtAsc(sprint.getProject()).stream()
                .filter(c -> inSprint(c, sprint))
                .toList();
    }

    public List<StudentStats> forSprint(Sprint sprint) {
        OffsetDateTime from = sprintStart(sprint);
        int days = (int) Duration.between(from, sprintEnd(sprint)).toDays();
        return compute(students(sprint.getProject()), sprintCommits(sprint), taskRepository.findBySprint(sprint), from, days);
    }

    /** Whole-project totals (used by the viva export). Consistency is measured from the first to the last commit. */
    public List<StudentStats> forProject(Project project) {
        List<CodeCommit> commits = commitRepository.findByProjectOrderByCommittedAtAsc(project);
        OffsetDateTime from = commits.isEmpty() ? OffsetDateTime.now() : commits.get(0).getCommittedAt();
        OffsetDateTime to = commits.isEmpty() ? from : commits.get(commits.size() - 1).getCommittedAt();
        int days = (int) Duration.between(from, to).toDays() + 1;
        return compute(students(project), commits, taskRepository.findBySprint_Project(project), from, days);
    }

    /** Recomputes and stores this sprint's metrics, updating rows in place (unique on sprint + student). */
    @Transactional
    public List<StudentStats> computeAndSave(Sprint sprint) {
        List<StudentStats> stats = forSprint(sprint);
        Map<UUID, StudentContributionMetric> existing = metricRepository.findBySprint(sprint).stream()
                .collect(Collectors.toMap(m -> m.getStudent().getId(), Function.identity()));
        OffsetDateTime now = OffsetDateTime.now();

        for (StudentStats s : stats) {
            StudentContributionMetric m = existing.getOrDefault(s.student().getId(), new StudentContributionMetric());
            m.setProject(sprint.getProject());
            m.setSprint(sprint);
            m.setStudent(s.student());
            m.setTotalCommits(s.commits());
            m.setLinesAdded(s.linesAdded());
            m.setLinesDeleted(s.linesDeleted());
            m.setTasksCompleted(s.tasksCompleted());
            m.setConsistencyScore(s.consistency());
            m.setContributionPercentage(s.share() * 100);
            m.setComputedAt(now);
            metricRepository.save(m);
        }
        return stats;
    }

    // ---- pure math, unit-tested in ContributionMathTest ----

    /** Net code churn: deletions count half, so refactors still earn credit. */
    static double volume(int linesAdded, int linesDeleted) {
        return linesAdded + 0.5 * linesDeleted;
    }

    /**
     * Consistency index C = 1 - sigma/mu over daily commit-count buckets, clamped to [0, 1].
     * ~1 = steady daily work, ~0 = everything in one spike (or no commits).
     */
    static double consistency(List<OffsetDateTime> commitTimes, OffsetDateTime from, int buckets) {
        if (commitTimes.isEmpty() || buckets < 1) return 0;
        double[] counts = new double[buckets];
        for (OffsetDateTime t : commitTimes) {
            long day = Duration.between(from, t).toDays();
            counts[(int) Math.max(0, Math.min(buckets - 1, day))]++;
        }
        double mean = (double) commitTimes.size() / buckets;
        double variance = 0;
        for (double c : counts) variance += (c - mean) * (c - mean);
        double sigma = Math.sqrt(variance / buckets);
        return Math.max(0, Math.min(1, 1 - sigma / mean));
    }

    static List<StudentStats> compute(List<User> students, List<CodeCommit> commits, List<ProjectTask> tasks,
                                      OffsetDateTime from, int buckets) {
        Map<UUID, List<CodeCommit>> byAuthor = commits.stream()
                .collect(Collectors.groupingBy(c -> c.getAuthor().getId()));

        double totalVolume = commits.stream().mapToDouble(c -> volume(c.getLinesAdded(), c.getLinesDeleted())).sum();

        return students.stream().map(s -> {
            List<CodeCommit> mine = byAuthor.getOrDefault(s.getId(), List.of());
            List<ProjectTask> assigned = tasks.stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(s.getId()))
                    .toList();
            List<ProjectTask> done = assigned.stream().filter(t -> "DONE".equals(t.getStatus())).toList();

            int added = mine.stream().mapToInt(CodeCommit::getLinesAdded).sum();
            int deleted = mine.stream().mapToInt(CodeCommit::getLinesDeleted).sum();
            double vol = volume(added, deleted);

            return new StudentStats(s, mine.size(), added, deleted, assigned.size(), done.size(),
                    done.stream().mapToInt(ProjectTask::getStoryPoints).sum(), vol,
                    consistency(mine.stream().map(CodeCommit::getCommittedAt).toList(), from, buckets),
                    totalVolume == 0 ? 0 : vol / totalVolume);
        }).toList();
    }
}
