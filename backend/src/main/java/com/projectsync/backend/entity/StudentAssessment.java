package com.projectsync.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// ponytail: keyed by user_id like StudentProfile, so one assessment per student without a separate id + @OneToOne.
@Entity
@Table(name = "student_assessments")
public class StudentAssessment {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "frontend_score", nullable = false)
    private Integer frontendScore;

    @Column(name = "backend_score", nullable = false)
    private Integer backendScore;

    @Column(name = "database_score", nullable = false)
    private Integer databaseScore;

    @Column(name = "devops_score", nullable = false)
    private Integer devopsScore;

    @Column(name = "ml_ai_score", nullable = false)
    private Integer mlAiScore;

    @Column(name = "primary_interest", length = 50)
    private String primaryInterest;

    @Column(name = "past_projects", columnDefinition = "TEXT")
    private String pastProjects;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public StudentAssessment() {}

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Integer getFrontendScore() { return frontendScore; }
    public void setFrontendScore(Integer frontendScore) { this.frontendScore = frontendScore; }

    public Integer getBackendScore() { return backendScore; }
    public void setBackendScore(Integer backendScore) { this.backendScore = backendScore; }

    public Integer getDatabaseScore() { return databaseScore; }
    public void setDatabaseScore(Integer databaseScore) { this.databaseScore = databaseScore; }

    public Integer getDevopsScore() { return devopsScore; }
    public void setDevopsScore(Integer devopsScore) { this.devopsScore = devopsScore; }

    public Integer getMlAiScore() { return mlAiScore; }
    public void setMlAiScore(Integer mlAiScore) { this.mlAiScore = mlAiScore; }

    public String getPrimaryInterest() { return primaryInterest; }
    public void setPrimaryInterest(String primaryInterest) { this.primaryInterest = primaryInterest; }

    public String getPastProjects() { return pastProjects; }
    public void setPastProjects(String pastProjects) { this.pastProjects = pastProjects; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
