package com.projectsync.backend.repository;

import com.projectsync.backend.entity.FacultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, UUID> {
}