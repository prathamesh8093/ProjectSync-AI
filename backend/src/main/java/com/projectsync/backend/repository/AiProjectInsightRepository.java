package com.projectsync.backend.repository;

import com.projectsync.backend.entity.AiProjectInsight;
import com.projectsync.backend.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiProjectInsightRepository extends JpaRepository<AiProjectInsight, UUID> {
    Optional<AiProjectInsight> findFirstBySprintOrderByGeneratedAtDesc(Sprint sprint);
}
