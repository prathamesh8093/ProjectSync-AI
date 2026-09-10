package com.projectsync.backend.service;

import com.projectsync.backend.dto.StudentProfileRequest;
import com.projectsync.backend.dto.StudentProfileResponse;
import com.projectsync.backend.entity.StudentProfile;
import com.projectsync.backend.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentProfileService {

    private final StudentProfileRepository repository;

    public StudentProfileService(StudentProfileRepository repository) {
        this.repository = repository;
    }

    public Optional<StudentProfile> getByUserId(UUID userId) {
        return repository.findById(userId);
    }

    public StudentProfile upsert(UUID userId, StudentProfileRequest request) {
        StudentProfile profile = repository.findById(userId).orElseGet(StudentProfile::new);

        OffsetDateTime now = OffsetDateTime.now();
        boolean isNew = profile.getUserId() == null;

        if (isNew) {
            profile.setUserId(userId);
            profile.setCreatedAt(now);
        }

        profile.setStudentId(request.getStudentId());
        profile.setDepartment(request.getDepartment());
        profile.setYear(request.getYear());
        profile.setBio(request.getBio());
        profile.setUpdatedAt(now);

        return repository.save(profile);
    }

    public StudentProfileResponse toResponse(StudentProfile profile) {
        StudentProfileResponse response = new StudentProfileResponse();
        response.setUserId(profile.getUserId());
        response.setStudentId(profile.getStudentId());
        response.setDepartment(profile.getDepartment());
        response.setYear(profile.getYear());
        response.setBio(profile.getBio());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}