package com.projectsync.backend.controller;

import com.projectsync.backend.vcs.dto.*;
import com.projectsync.backend.vcs.service.GitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vcs")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class VcsController {

    private final GitService gitService;

    public VcsController(GitService gitService) {
        this.gitService = gitService;
    }

    private String resolveUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "local-developer";
    }

    private boolean isFacultyOrAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equalsIgnoreCase("ROLE_FACULTY") ||
                a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
        );
    }

    // --------------------------------------------------------------------------
    // HEALTH & DIAGNOSTICS
    // --------------------------------------------------------------------------

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ProjectSync-AI VCS Engine",
                "profile", System.getProperty("spring.profiles.active", "local"),
                "trackedRepositories", gitService.listRepositories().size()));
    }

    // --------------------------------------------------------------------------
    // REPOSITORIES
    // --------------------------------------------------------------------------

    @GetMapping("/repositories")
    public ResponseEntity<List<RepositorySummaryDto>> listRepositories(Authentication auth) {
        String username = resolveUsername(auth);
        boolean isFacultyOrAdmin = isFacultyOrAdmin(auth);
        return ResponseEntity.ok(gitService.listRepositories(username, isFacultyOrAdmin));
    }

    @PostMapping("/repositories")
    public ResponseEntity<RepositoryDetailsDto> createRepository(
            @Valid @RequestBody CreateRepoRequest req,
            Authentication auth) {
        String username = resolveUsername(auth);
        RepositoryDetailsDto dto = gitService.createRepository(req, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/repositories/register")
    public ResponseEntity<RepositoryDetailsDto> registerExistingRepository(
            @Valid @RequestBody RegisterRepoRequest req,
            Authentication auth) {
        String username = resolveUsername(auth);
        RepositoryDetailsDto dto = gitService.registerExistingRepository(req, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/repositories/clone")
    public ResponseEntity<CloneResultDto> cloneRepository(
            @Valid @RequestBody CloneRepoRequest req,
            Authentication auth) {
        String username = resolveUsername(auth);
        CloneResultDto result = gitService.cloneRepository(req, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/repositories/{id}")
    public ResponseEntity<RepositoryDetailsDto> getRepositoryDetails(@PathVariable String id) {
        return ResponseEntity.ok(gitService.getRepositoryDetails(id));
    }

    @PutMapping("/repositories/{id}")
    public ResponseEntity<Map<String, String>> updateRepository(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        gitService.updateRepository(id, body.get("name"), body.get("description"));
        return ResponseEntity.ok(Map.of("message", "Repository updated successfully"));
    }

    @DeleteMapping("/repositories/{id}")
    public ResponseEntity<Map<String, String>> deleteRepository(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean deleteFiles) {
        gitService.deleteRepository(id, deleteFiles);
        return ResponseEntity.ok(Map.of("message", "Repository unregistered successfully"));
    }

    // --------------------------------------------------------------------------
    // STATUS & STAGING
    // --------------------------------------------------------------------------

    @GetMapping("/repositories/{id}/status")
    public ResponseEntity<WorkingTreeStatusDto> getStatus(@PathVariable String id) {
        return ResponseEntity.ok(gitService.getStatus(id));
    }

    @PostMapping("/repositories/{id}/stage")
    public ResponseEntity<Map<String, String>> stageFiles(
            @PathVariable String id,
            @RequestBody StageFilesRequest req) {
        gitService.stageFiles(id, req.getFilePaths());
        return ResponseEntity.ok(Map.of("message", "Files staged successfully"));
    }

    @PostMapping("/repositories/{id}/stage-all")
    public ResponseEntity<Map<String, String>> stageAll(@PathVariable String id) {
        gitService.stageAll(id);
        return ResponseEntity.ok(Map.of("message", "All changes staged successfully"));
    }

    @PostMapping("/repositories/{id}/unstage")
    public ResponseEntity<Map<String, String>> unstageFiles(
            @PathVariable String id,
            @RequestBody StageFilesRequest req) {
        gitService.unstageFiles(id, req.getFilePaths());
        return ResponseEntity.ok(Map.of("message", "Files unstaged successfully"));
    }

    @PostMapping("/repositories/{id}/unstage-all")
    public ResponseEntity<Map<String, String>> unstageAll(@PathVariable String id) {
        gitService.unstageAll(id);
        return ResponseEntity.ok(Map.of("message", "All changes unstaged successfully"));
    }

    @PostMapping("/repositories/{id}/discard")
    public ResponseEntity<Map<String, String>> discardChanges(
            @PathVariable String id,
            @RequestBody StageFilesRequest req) {
        gitService.discardChanges(id, req.getFilePaths());
        return ResponseEntity.ok(Map.of("message", "Changes discarded successfully"));
    }

    // --------------------------------------------------------------------------
    // COMMITS & HISTORY
    // --------------------------------------------------------------------------

    @PostMapping("/repositories/{id}/commit")
    public ResponseEntity<CommitDto> createCommit(
            @PathVariable String id,
            @Valid @RequestBody CommitRequest req,
            Authentication auth) {
        String username = resolveUsername(auth);
        String email = username + "@projectsync.local";
        CommitDto dto = gitService.createCommit(id, req, username, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/repositories/{id}/commits")
    public ResponseEntity<List<CommitDto>> getCommitHistory(
            @PathVariable String id,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(gitService.getCommitHistory(id, branch, limit));
    }

    @GetMapping("/repositories/{id}/commits/{sha}")
    public ResponseEntity<CommitDetailsDto> getCommitDetails(
            @PathVariable String id,
            @PathVariable String sha) {
        return ResponseEntity.ok(gitService.getCommitDetails(id, sha));
    }

    // --------------------------------------------------------------------------
    // BRANCHES
    // --------------------------------------------------------------------------

    @GetMapping("/repositories/{id}/branches")
    public ResponseEntity<List<BranchDto>> listBranches(@PathVariable String id) {
        return ResponseEntity.ok(gitService.listBranches(id));
    }

    @PostMapping("/repositories/{id}/branches")
    public ResponseEntity<BranchDto> createBranch(
            @PathVariable String id,
            @Valid @RequestBody CreateBranchRequest req) {
        BranchDto dto = gitService.createBranch(id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/repositories/{id}/branches/checkout")
    public ResponseEntity<Map<String, String>> checkoutBranch(
            @PathVariable String id,
            @RequestParam String name) {
        gitService.checkoutBranch(id, name);
        return ResponseEntity.ok(Map.of("message", "Checked out branch: " + name));
    }

    @DeleteMapping("/repositories/{id}/branches/{name}")
    public ResponseEntity<Map<String, String>> deleteBranch(
            @PathVariable String id,
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean force) {
        gitService.deleteBranch(id, name, force);
        return ResponseEntity.ok(Map.of("message", "Deleted branch: " + name));
    }

    // --------------------------------------------------------------------------
    // DIFF
    // --------------------------------------------------------------------------

    @GetMapping("/repositories/{id}/diff")
    public ResponseEntity<DiffDto> getDiff(
            @PathVariable String id,
            @RequestParam(required = false) String path,
            @RequestParam(defaultValue = "false") boolean staged) {
        return ResponseEntity.ok(gitService.getWorkingTreeDiff(id, path, staged));
    }

    // --------------------------------------------------------------------------
    // REMOTES & GITHUB SYNC
    // --------------------------------------------------------------------------

    @GetMapping("/repositories/{id}/remotes")
    public ResponseEntity<List<RemoteConfigDto>> getRemotes(@PathVariable String id) {
        return ResponseEntity.ok(gitService.getRemotes(id));
    }

    @PostMapping("/repositories/{id}/remotes")
    public ResponseEntity<Map<String, String>> setRemote(
            @PathVariable String id,
            @Valid @RequestBody RemoteConfigDto req) {
        gitService.setRemote(id, req);
        return ResponseEntity.ok(Map.of("message", "Remote configured successfully"));
    }

    @PostMapping("/repositories/{id}/fetch")
    public ResponseEntity<SyncResultDto> fetchRemote(
            @PathVariable String id,
            @RequestBody SyncRequest req) {
        return ResponseEntity.ok(gitService.fetchRemote(id, req));
    }

    @PostMapping("/repositories/{id}/pull")
    public ResponseEntity<SyncResultDto> pullRemote(
            @PathVariable String id,
            @RequestBody SyncRequest req) {
        return ResponseEntity.ok(gitService.pullRemote(id, req));
    }

    @PostMapping("/repositories/{id}/push")
    public ResponseEntity<SyncResultDto> pushRemote(
            @PathVariable String id,
            @RequestBody SyncRequest req) {
        return ResponseEntity.ok(gitService.pushRemote(id, req));
    }
}
