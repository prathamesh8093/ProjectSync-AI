package com.projectsync.backend.controller;

import com.projectsync.backend.dto.WorkloadResponse;
import com.projectsync.backend.service.WorkloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workload")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping("/my-project")
    public ResponseEntity<List<WorkloadResponse>> getMyProjectWorkload(
            Authentication authentication) {

        List<WorkloadResponse> workload =
                workloadService.getMyProjectWorkload(authentication.getName());

        return ResponseEntity.ok(workload);
    }
}