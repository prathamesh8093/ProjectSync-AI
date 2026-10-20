package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.StudentContributionMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentContributionMetricRepository extends JpaRepository<StudentContributionMetric, UUID> {
    List<StudentContributionMetric> findBySprint(Sprint sprint);
    void deleteBySprint(Sprint sprint);
}
