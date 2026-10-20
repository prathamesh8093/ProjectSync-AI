package com.projectsync.backend.repository;

import com.projectsync.backend.entity.CodeCommit;
import com.projectsync.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CodeCommitRepository extends JpaRepository<CodeCommit, UUID> {
    List<CodeCommit> findByProjectOrderByCommittedAtAsc(Project project);
    boolean existsByProjectAndSha(Project project, String sha);
}
