package com.projectsync.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_contribution_metrics")
public class StudentContributionMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id")
    private User student;

    @Column(name = "total_commits", nullable = false)
    private Integer totalCommits;

    @Column(name = "lines_added", nullable = false)
    private Integer linesAdded;

    @Column(name = "lines_deleted", nullable = false)
    private Integer linesDeleted;

    @Column(name = "tasks_completed", nullable = false)
    private Integer tasksCompleted;

    @Column(name = "consistency_score", nullable = false)
    private Double consistencyScore;

    @Column(name = "contribution_percentage", nullable = false)
    private Double contributionPercentage;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    public StudentContributionMetric() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Sprint getSprint() { return sprint; }
    public void setSprint(Sprint sprint) { this.sprint = sprint; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Integer getTotalCommits() { return totalCommits; }
    public void setTotalCommits(Integer totalCommits) { this.totalCommits = totalCommits; }

    public Integer getLinesAdded() { return linesAdded; }
    public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }

    public Integer getLinesDeleted() { return linesDeleted; }
    public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }

    public Integer getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(Integer tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public Double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(Double consistencyScore) { this.consistencyScore = consistencyScore; }

    public Double getContributionPercentage() { return contributionPercentage; }
    public void setContributionPercentage(Double contributionPercentage) { this.contributionPercentage = contributionPercentage; }

    public OffsetDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(OffsetDateTime computedAt) { this.computedAt = computedAt; }
}
