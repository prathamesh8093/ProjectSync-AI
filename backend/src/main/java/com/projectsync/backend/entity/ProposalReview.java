package com.projectsync.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "proposal_reviews")
public class ProposalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private ProjectProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_user_id", nullable = false)
    private User facultyUser;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    public ProposalReview() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ProjectProposal getProposal() { return proposal; }
    public void setProposal(ProjectProposal proposal) { this.proposal = proposal; }

    public User getFacultyUser() { return facultyUser; }
    public void setFacultyUser(User facultyUser) { this.facultyUser = facultyUser; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public OffsetDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(OffsetDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}