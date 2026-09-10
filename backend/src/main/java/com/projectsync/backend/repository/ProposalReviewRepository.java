package com.projectsync.backend.repository;

import com.projectsync.backend.entity.ProjectProposal;
import com.projectsync.backend.entity.ProposalReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProposalReviewRepository extends JpaRepository<ProposalReview, UUID> {
    Optional<ProposalReview> findByProposal(ProjectProposal proposal);
}