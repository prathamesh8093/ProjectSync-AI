package com.projectsync.backend.controller;

import com.projectsync.backend.entity.CodeCommit;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectTask;
import com.projectsync.backend.entity.Sprint;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.CodeCommitRepository;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.ProjectTaskRepository;
import com.projectsync.backend.repository.SprintRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ponytail: minimal data entry for sprints, Kanban tasks and commits so analytics has something to read.
 * Delete once member2-agile (Kanban UI) and a GitHub integration provide this data.
 * Only members of the project's team may write.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class AgileStubController {

    public record SprintRequest(@NotBlank @Size(max = 100) String name, @NotNull LocalDate startDate,
                                @NotNull LocalDate endDate, @Min(0) Integer plannedPoints) {}

    public record TaskRequest(@NotNull UUID sprintId, @NotBlank @Size(max = 200) String title,
                              UUID assigneeUserId, @Min(0) Integer storyPoints) {}

    public record StatusRequest(@NotBlank String status) {}

    public record CommitRequest(String sha, String message, String authorEmail,
                                Integer linesAdded, Integer linesDeleted, OffsetDateTime committedAt) {}

    private static final Set<String> STATUSES = Set.of("TODO", "IN_PROGRESS", "DONE");

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SprintRepository sprintRepository;
    private final ProjectTaskRepository taskRepository;
    private final CodeCommitRepository commitRepository;

    public AgileStubController(UserRepository userRepository,
                               ProjectRepository projectRepository,
                               TeamMemberRepository teamMemberRepository,
                               SprintRepository sprintRepository,
                               ProjectTaskRepository taskRepository,
                               CodeCommitRepository commitRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.commitRepository = commitRepository;
    }

    @GetMapping("/sprints")
    public ResponseEntity<?> listSprints(Authentication auth, @PathVariable UUID projectId) {
        Project project = teamProject(auth, projectId);
        if (project == null) return notMember();
        return ResponseEntity.ok(sprintRepository.findByProjectOrderByStartDateAsc(project).stream()
                .map(s -> Map.of("id", s.getId(), "name", s.getName(), "startDate", s.getStartDate(),
                        "endDate", s.getEndDate(), "plannedPoints", s.getPlannedPoints()))
                .toList());
    }

    @PostMapping("/sprints")
    public ResponseEntity<?> createSprint(Authentication auth, @PathVariable UUID projectId,
                                          @Valid @RequestBody SprintRequest request) {
        Project project = teamProject(auth, projectId);
        if (project == null) return notMember();
        if (request.endDate().isBefore(request.startDate())) {
            return ResponseEntity.badRequest().body("endDate must be on or after startDate.");
        }
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName(request.name());
        sprint.setStartDate(request.startDate());
        sprint.setEndDate(request.endDate());
        sprint.setPlannedPoints(request.plannedPoints() == null ? 0 : request.plannedPoints());
        sprint = sprintRepository.save(sprint);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", sprint.getId()));
    }

    @PostMapping("/tasks")
    public ResponseEntity<?> createTask(Authentication auth, @PathVariable UUID projectId,
                                        @Valid @RequestBody TaskRequest request) {
        Project project = teamProject(auth, projectId);
        if (project == null) return notMember();

        Sprint sprint = sprintRepository.findById(request.sprintId()).orElse(null);
        if (sprint == null || !sprint.getProject().getId().equals(projectId)) {
            return ResponseEntity.badRequest().body("Sprint does not belong to this project.");
        }

        User assignee = null;
        if (request.assigneeUserId() != null) {
            assignee = teamStudents(project).stream()
                    .filter(u -> u.getId().equals(request.assigneeUserId())).findFirst().orElse(null);
            if (assignee == null) return ResponseEntity.badRequest().body("Assignee is not on this team.");
        }

        ProjectTask task = new ProjectTask();
        task.setSprint(sprint);
        task.setTitle(request.title());
        task.setAssignee(assignee);
        task.setStoryPoints(request.storyPoints() == null ? 0 : request.storyPoints());
        task = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", task.getId()));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(Authentication auth, @PathVariable UUID projectId,
                                              @PathVariable UUID taskId, @Valid @RequestBody StatusRequest request) {
        Project project = teamProject(auth, projectId);
        if (project == null) return notMember();
        if (!STATUSES.contains(request.status())) {
            return ResponseEntity.badRequest().body("status must be one of " + STATUSES + ".");
        }

        ProjectTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !task.getSprint().getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found.");
        }
        task.setStatus(request.status());
        task.setCompletedAt("DONE".equals(request.status()) ? OffsetDateTime.now() : null);
        taskRepository.save(task);
        return ResponseEntity.ok(Map.of("id", task.getId(), "status", task.getStatus()));
    }

    /** Bulk import, e.g. from `git log --numstat`. Commits whose author email isn't on the team are skipped. */
    @PostMapping("/commits")
    public ResponseEntity<?> importCommits(Authentication auth, @PathVariable UUID projectId,
                                           @RequestBody List<CommitRequest> request) {
        Project project = teamProject(auth, projectId);
        if (project == null) return notMember();

        Map<String, User> byEmail = teamStudents(project).stream()
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u));

        int imported = 0, skipped = 0;
        for (CommitRequest c : request) {
            User author = c.authorEmail() == null ? null : byEmail.get(c.authorEmail().toLowerCase());
            boolean valid = author != null && c.sha() != null && !c.sha().isBlank() && c.sha().length() <= 64
                    && c.committedAt() != null
                    && (c.linesAdded() == null || c.linesAdded() >= 0)
                    && (c.linesDeleted() == null || c.linesDeleted() >= 0);
            if (!valid || commitRepository.existsByProjectAndSha(project, c.sha())) {
                skipped++;
                continue;
            }
            CodeCommit commit = new CodeCommit();
            commit.setProject(project);
            commit.setAuthor(author);
            commit.setSha(c.sha());
            commit.setMessage(c.message());
            commit.setLinesAdded(c.linesAdded() == null ? 0 : c.linesAdded());
            commit.setLinesDeleted(c.linesDeleted() == null ? 0 : c.linesDeleted());
            commit.setCommittedAt(c.committedAt());
            commitRepository.save(commit);
            imported++;
        }
        return ResponseEntity.ok(Map.of("imported", imported, "skipped", skipped));
    }

    /** The project, if the caller is a student on its team; otherwise null. */
    private Project teamProject(Authentication auth, UUID projectId) {
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        Project project = projectRepository.findById(projectId).orElse(null);
        if (user == null || project == null) return null;
        return teamMemberRepository.findByTeamAndStudentUser(project.getTeam(), user).isPresent() ? project : null;
    }

    private List<User> teamStudents(Project project) {
        return teamMemberRepository.findByTeam(project.getTeam()).stream().map(TeamMember::getStudentUser).toList();
    }

    private ResponseEntity<?> notMember() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Project not found or you are not on its team.");
    }
}
