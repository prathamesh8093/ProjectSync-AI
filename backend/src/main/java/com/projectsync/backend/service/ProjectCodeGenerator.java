package com.projectsync.backend.service;

import com.projectsync.backend.repository.ProjectProposalRepository;
import com.projectsync.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class ProjectCodeGenerator {

    private final ProjectProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;

    public ProjectCodeGenerator(ProjectProposalRepository proposalRepository,
                                ProjectRepository projectRepository) {
        this.proposalRepository = proposalRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Generates a clean, unique, and human-understandable Project ID in format: PRJ-YYYY-XXX
     * (e.g. PRJ-2026-001, PRJ-2026-002).
     * Guarantees uniqueness against both project_proposals and projects.
     */
    public synchronized String generateNextProjectCode() {
        int currentYear = Year.now().getValue();
        String prefix = "PRJ-" + currentYear + "-";

        long baseCount = proposalRepository.count();
        long index = baseCount + 1;

        String candidate = String.format("%s%03d", prefix, index);
        while (proposalRepository.existsByProjectCode(candidate) || projectRepository.existsByProjectCode(candidate)) {
            index++;
            candidate = String.format("%s%03d", prefix, index);
        }

        return candidate;
    }
}

