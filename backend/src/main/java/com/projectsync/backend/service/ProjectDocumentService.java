package com.projectsync.backend.service;

import com.projectsync.backend.dto.CreateProjectDocumentRequest;
import com.projectsync.backend.dto.ProjectDocumentResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectDocument;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectDocumentRepository;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectDocumentService {

    private static final String APPROVED = "APPROVED";

    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public ProjectDocumentService(
            ProjectDocumentRepository projectDocumentRepository,
            ProjectRepository projectRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {

        this.projectDocumentRepository = projectDocumentRepository;
        this.projectRepository = projectRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectDocumentResponse createDocument(
            CreateProjectDocumentRequest request,
            String email) {

        User user = resolveStudent(email);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found"));

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        String title = request.getTitle().trim();

        if (title.isBlank()) {
            throw new ConflictException(
                    "Document title cannot be blank");
        }

        ProjectDocument document = new ProjectDocument();

        document.setProject(project);
        document.setTitle(title);
        document.setContent(request.getContent());
        document.setCreatedBy(user);
        document.setUpdatedBy(user);

        OffsetDateTime now = OffsetDateTime.now();

        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        ProjectDocument saved =
                projectDocumentRepository.save(document);

        return toResponse(saved);
    }

    @Transactional
    public List<ProjectDocumentResponse> getMyProjectDocuments(
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        return projectDocumentRepository
                .findByProjectOrderByUpdatedAtDesc(project)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDocumentResponse getDocumentById(
            UUID documentId,
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        ProjectDocument document =
                projectDocumentRepository
                        .findByIdAndProject(documentId, project)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project document not found"));

        return toResponse(document);
    }

    @Transactional
    public ProjectDocumentResponse updateDocument(
            UUID documentId,
            CreateProjectDocumentRequest request,
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        ProjectDocument document =
                projectDocumentRepository
                        .findByIdAndProject(documentId, project)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project document not found"));

        String title = request.getTitle().trim();

        if (title.isBlank()) {
            throw new ConflictException(
                    "Document title cannot be blank");
        }

        document.setTitle(title);
        document.setContent(request.getContent());
        document.setUpdatedBy(user);
        document.setUpdatedAt(OffsetDateTime.now());

        ProjectDocument saved =
                projectDocumentRepository.save(document);

        return toResponse(saved);
    }

    @Transactional
    public void deleteDocument(
            UUID documentId,
            String email) {

        User user = resolveStudent(email);

        Project project = resolveMyProject(user);

        requireApprovedProject(project);
        requireTeamMembership(project.getTeam(), user);

        ProjectDocument document =
                projectDocumentRepository
                        .findByIdAndProject(documentId, project)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project document not found"));

        projectDocumentRepository.delete(document);
    }

    private User resolveStudent(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"));
    }

    private Project resolveMyProject(User user) {

        TeamMember membership =
                teamMemberRepository
                        .findByStudentUser(user)
                        .orElseThrow(() ->
                                new ForbiddenOperationException(
                                        "You are not a member of a team"));

        Team team = membership.getTeam();

        return projectRepository.findByTeam(team)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No Agile project found for your team"));
    }

    private void requireTeamMembership(
            Team team,
            User user) {

        teamMemberRepository
                .findByTeamAndStudentUser(team, user)
                .orElseThrow(() ->
                        new ForbiddenOperationException(
                                "You are not a member of this project team"));
    }

    private void requireApprovedProject(
            Project project) {

        if (project.getProposal() == null
                || project.getProposal().getStatus() == null
                || !APPROVED.equalsIgnoreCase(
                        project.getProposal().getStatus())) {

            throw new ForbiddenOperationException(
                    "Agile workspace is available only for approved proposals");
        }
    }

    private ProjectDocumentResponse toResponse(
            ProjectDocument document) {

        ProjectDocumentResponse response =
                new ProjectDocumentResponse();

        response.setId(document.getId());

        response.setProjectId(
                document.getProject().getId());

        response.setTitle(
                document.getTitle());

        response.setContent(
                document.getContent());

        User createdBy =
                document.getCreatedBy();

        if (createdBy != null) {

            response.setCreatedBy(
                    createdBy.getId());

            response.setCreatedByName(
                    createdBy.getFullName());

            response.setCreatedByEmail(
                    createdBy.getEmail());
        }

        User updatedBy =
                document.getUpdatedBy();

        if (updatedBy != null) {

            response.setUpdatedBy(
                    updatedBy.getId());

            response.setUpdatedByName(
                    updatedBy.getFullName());

            response.setUpdatedByEmail(
                    updatedBy.getEmail());
        }

        response.setCreatedAt(
                document.getCreatedAt());

        response.setUpdatedAt(
                document.getUpdatedAt());

        return response;
    }
}