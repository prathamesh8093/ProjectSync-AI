package com.projectsync.backend.service;

import com.projectsync.backend.dto.ProjectFileResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectFile;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectFileRepository;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectFileService {

    private static final String APPROVED = "APPROVED";

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final SupabaseStorageService supabaseStorageService;

    public ProjectFileService(
            ProjectFileRepository projectFileRepository,
            ProjectRepository projectRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            SupabaseStorageService supabaseStorageService) {

        this.projectFileRepository = projectFileRepository;
        this.projectRepository = projectRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Transactional
    public ProjectFileResponse uploadFile(
            MultipartFile file,
            String email) {

        User user = resolveStudent(email);

        validateFile(file);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        String fileName = normalizeFileName(
                file.getOriginalFilename()
        );

        String contentType = normalizeNullable(
                file.getContentType()
        );

        long fileSize = file.getSize();

        UUID fileId = UUID.randomUUID();

        String storagePath = buildStoragePath(
                project.getId(),
                fileId,
                fileName
        );

        byte[] content;

        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read uploaded file",
                    exception
            );
        }

        ProjectFile projectFile = new ProjectFile();

        projectFile.setId(fileId);
        projectFile.setProject(project);
        projectFile.setFileName(fileName);
        projectFile.setStoragePath(storagePath);
        projectFile.setContentType(contentType);
        projectFile.setFileSize(fileSize);
        projectFile.setUploadedBy(user);

        boolean uploadedToStorage = false;

        try {
            supabaseStorageService.upload(
                    storagePath,
                    content,
                    contentType
            );

            uploadedToStorage = true;

            ProjectFile saved =
                    projectFileRepository.save(projectFile);

            return toResponse(saved);

        } catch (RuntimeException exception) {

            if (uploadedToStorage) {
                try {
                    supabaseStorageService.delete(storagePath);
                } catch (RuntimeException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }

            throw exception;
        }
    }

    @Transactional
    public List<ProjectFileResponse> getMyProjectFiles(
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        return projectFileRepository
                .findByProjectIdOrderByCreatedAtDesc(project.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectFileResponse getFile(
            UUID fileId,
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        ProjectFile projectFile =
                findProjectFile(fileId, project.getId());

        return toResponse(projectFile);
    }

    @Transactional
    public DownloadedProjectFile downloadFile(
            UUID fileId,
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        ProjectFile projectFile =
                findProjectFile(fileId, project.getId());

        byte[] content =
                supabaseStorageService.download(
                        projectFile.getStoragePath()
                );

        return new DownloadedProjectFile(
                projectFile.getFileName(),
                projectFile.getContentType(),
                content
        );
    }

    @Transactional
    public void deleteFile(
            UUID fileId,
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        ProjectFile projectFile =
                findProjectFile(fileId, project.getId());

        supabaseStorageService.delete(
                projectFile.getStoragePath()
        );

        projectFileRepository.delete(projectFile);
    }

    private ProjectFile findProjectFile(
            UUID fileId,
            UUID projectId) {

        if (fileId == null) {
            throw new IllegalArgumentException(
                    "File ID is required"
            );
        }

        return projectFileRepository
                .findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project file not found"
                        )
                );
    }

    private User resolveStudent(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        if (!"STUDENT".equals(user.getRole())) {
            throw new ForbiddenOperationException(
                    "Only students can access this resource"
            );
        }

        return user;
    }

    private Project resolveMyProject(User user) {

        TeamMember membership =
                teamMemberRepository
                        .findByStudentUser(user)
                        .orElseThrow(() ->
                                new ForbiddenOperationException(
                                        "You are not a member of a team"
                                )
                        );

        Team team = membership.getTeam();

        return projectRepository
                .findByTeam(team)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No Agile project found for your team"
                        )
                );
    }

    private void requireTeamMembership(
            Team team,
            User user) {

        teamMemberRepository
                .findByTeamAndStudentUser(team, user)
                .orElseThrow(() ->
                        new ForbiddenOperationException(
                                "You are not a member of this project team"
                        )
                );
    }

    private void requireApprovedProject(
            Project project) {

        if (project.getProposal() == null
                || project.getProposal().getStatus() == null
                || !APPROVED.equalsIgnoreCase(
                        project.getProposal().getStatus()
                )) {

            throw new ForbiddenOperationException(
                    "Agile workspace is available only for approved proposals"
            );
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File is required"
            );
        }

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new IllegalArgumentException(
                    "File name is required"
            );
        }
    }

    private String normalizeFileName(
            String fileName) {

        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "File name is required"
            );
        }

        String normalized = fileName.trim();

        if (normalized.length() > 255) {
            throw new IllegalArgumentException(
                    "File name must not exceed 255 characters"
            );
        }

        if (normalized.contains("/")
                || normalized.contains("\\")
                || normalized.contains("..")) {

            throw new IllegalArgumentException(
                    "Invalid file name"
            );
        }

        return normalized;
    }

    private String normalizeNullable(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String buildStoragePath(
            UUID projectId,
            UUID fileId,
            String fileName) {

        return projectId
                + "/"
                + fileId
                + "/"
                + fileName;
    }

    private ProjectFileResponse toResponse(
            ProjectFile projectFile) {

        ProjectFileResponse response =
                new ProjectFileResponse();

        response.setId(projectFile.getId());

        if (projectFile.getProject() != null) {
            response.setProjectId(
                    projectFile.getProject().getId()
            );
        }

        response.setFileName(
                projectFile.getFileName()
        );

        response.setStoragePath(
                projectFile.getStoragePath()
        );

        response.setContentType(
                projectFile.getContentType()
        );

        response.setFileSize(
                projectFile.getFileSize()
        );

        User uploadedBy =
                projectFile.getUploadedBy();

        if (uploadedBy != null) {
            response.setUploadedBy(
                    uploadedBy.getId()
            );

            response.setUploadedByName(
                    uploadedBy.getFullName()
            );

            response.setUploadedByEmail(
                    uploadedBy.getEmail()
            );
        }

        response.setCreatedAt(
                projectFile.getCreatedAt()
        );

        response.setUpdatedAt(
                projectFile.getUpdatedAt()
        );

        return response;
    }

    public record DownloadedProjectFile(
            String fileName,
            String contentType,
            byte[] content) {
    }
}