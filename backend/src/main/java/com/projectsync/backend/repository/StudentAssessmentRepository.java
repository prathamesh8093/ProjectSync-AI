package com.projectsync.backend.repository;

import com.projectsync.backend.entity.StudentAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StudentAssessmentRepository extends JpaRepository<StudentAssessment, UUID> {
}
