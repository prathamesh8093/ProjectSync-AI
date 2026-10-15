package com.projectsync.backend.controller;

import com.projectsync.backend.dto.ProjectFileResponse;
import com.projectsync.backend.service.ProjectFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project-files")
public class ProjectFileController {

    private final ProjectFileService projectFileService;

    public ProjectFileController(
            ProjectFileService projectFileService) {

        this.projectFileService = projectFileService;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProjectFileResponse> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        ProjectFileResponse response =
                projectFileService.uploadFile(
                        file,
                        authentication.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/my-project")
    public ResponseEntity<List<ProjectFileResponse>> getMyProjectFiles(
            Authentication authentication) {

        return ResponseEntity.ok(
                projectFileService.getMyProjectFiles(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ProjectFileResponse> getFile(
            Authentication authentication,
            @PathVariable UUID fileId) {

        return ResponseEntity.ok(
                projectFileService.getFile(
                        fileId,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            Authentication authentication,
            @PathVariable UUID fileId) {

        ProjectFileService.DownloadedProjectFile downloadedFile =
                projectFileService.downloadFile(
                        fileId,
                        authentication.getName()
                );

        MediaType mediaType = resolveMediaType(
                downloadedFile.contentType()
        );

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        "Content-Disposition",
                        "attachment; filename=\"" +
                                downloadedFile.fileName() +
                                "\""
                )
                .body(downloadedFile.content());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(
            Authentication authentication,
            @PathVariable UUID fileId) {

        projectFileService.deleteFile(
                fileId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    private MediaType resolveMediaType(String contentType) {

        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}