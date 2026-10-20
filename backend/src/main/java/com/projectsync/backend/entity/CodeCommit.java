package com.projectsync.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// ponytail: commits are imported via POST /api/projects/{id}/commits; swap for a GitHub webhook when needed.
@Entity
@Table(name = "code_commits")
public class CodeCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 64)
    private String sha;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "lines_added", nullable = false)
    private Integer linesAdded = 0;

    @Column(name = "lines_deleted", nullable = false)
    private Integer linesDeleted = 0;

    @Column(name = "committed_at", nullable = false)
    private OffsetDateTime committedAt;

    public CodeCommit() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Integer getLinesAdded() { return linesAdded; }
    public void setLinesAdded(Integer linesAdded) { this.linesAdded = linesAdded; }

    public Integer getLinesDeleted() { return linesDeleted; }
    public void setLinesDeleted(Integer linesDeleted) { this.linesDeleted = linesDeleted; }

    public OffsetDateTime getCommittedAt() { return committedAt; }
    public void setCommittedAt(OffsetDateTime committedAt) { this.committedAt = committedAt; }
}
