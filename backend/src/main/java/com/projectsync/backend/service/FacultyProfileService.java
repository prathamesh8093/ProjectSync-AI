package com.projectsync.backend.service;

import com.projectsync.backend.dto.FacultyProfileRequest;
import com.projectsync.backend.dto.FacultyProfileResponse;
import com.projectsync.backend.entity.FacultyProfile;
import com.projectsync.backend.repository.FacultyProfileRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class FacultyProfileService {

    private final FacultyProfileRepository repository;

    public FacultyProfileService(FacultyProfileRepository repository) {
        this.repository = repository;
    }

    public Optional<FacultyProfile> getByUserId(UUID userId) {
        return repository.findById(userId);
    }

    public FacultyProfile upsert(UUID userId, FacultyProfileRequest request) {
        FacultyProfile profile = repository.findById(userId).orElseGet(FacultyProfile::new);

        OffsetDateTime now = OffsetDateTime.now();
        boolean isNew = profile.getUserId() == null;

        if (isNew) {
            profile.setUserId(userId);
            profile.setCreatedAt(now);
        }

        profile.setFacultyId(request.getFacultyId());
        profile.setDepartment(request.getDepartment());
        profile.setDesignation(request.getDesignation());
        profile.setUpdatedAt(now);

        return repository.save(profile);
    }

    public FacultyProfileResponse toResponse(FacultyProfile profile) {
        FacultyProfileResponse response = new FacultyProfileResponse();
        response.setUserId(profile.getUserId());
        response.setFacultyId(profile.getFacultyId());
        response.setDepartment(profile.getDepartment());
        response.setDesignation(profile.getDesignation());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}