package com.projectsync.backend.vcs;

import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.vcs.dto.*;
import com.projectsync.backend.vcs.repository.LocalRepositoryMetadataStore;
import com.projectsync.backend.vcs.service.GitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GitServiceTest {

    @TempDir
    Path tempStorageDir;

    private GitService gitService;
    private LocalRepositoryMetadataStore metadataStore;

    @BeforeEach
    void setUp() {
        org.eclipse.jgit.storage.file.WindowCacheConfig config = new org.eclipse.jgit.storage.file.WindowCacheConfig();
        config.setPackedGitMMAP(false);
        config.install();

        metadataStore = new LocalRepositoryMetadataStore(tempStorageDir.toString());
        gitService = new GitService(tempStorageDir.toString(), metadataStore);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.eclipse.jgit.storage.file.WindowCacheConfig config = new org.eclipse.jgit.storage.file.WindowCacheConfig();
        config.setPackedGitMMAP(false);
        config.install();
    }

    @Test
    void testCreateRepositoryAndInitialCommit() {
        CreateRepoRequest req = new CreateRepoRequest("my-demo-repo", "A test demo repository", "main", true);
        RepositoryDetailsDto repo = gitService.createRepository(req, "test-user");

        assertNotNull(repo);
        assertNotNull(repo.getId());
        assertEquals("my-demo-repo", repo.getName());
        assertEquals("main", repo.getCurrentBranch());
        assertEquals(1, repo.getCommitCount()); // Initial commit
        assertTrue(repo.isClean());

        Path repoDir = Path.of(repo.getDiskPath());
        assertTrue(Files.exists(repoDir.resolve(".git")));
        assertTrue(Files.exists(repoDir.resolve("README.md")));

        // Check history
        List<CommitDto> history = gitService.getCommitHistory(repo.getId(), "main", 10);
        assertEquals(1, history.size());
        assertEquals("Initial commit", history.get(0).getMessage());
    }

    @Test
    void testPathTraversalPrevention() {
        CreateRepoRequest badReq = new CreateRepoRequest("../evil-path", "bad", "main", false);
        assertThrows(BadRequestException.class, () -> gitService.createRepository(badReq, "user"));

        CreateRepoRequest badChars = new CreateRepoRequest("evil/path", "bad", "main", false);
        assertThrows(BadRequestException.class, () -> gitService.createRepository(badChars, "user"));
    }

    @Test
    void testFileStagingCommitAndStatus() throws IOException {
        CreateRepoRequest req = new CreateRepoRequest("status-repo", "Testing status", "main", false);
        RepositoryDetailsDto repo = gitService.createRepository(req, "tester");
        Path repoPath = Path.of(repo.getDiskPath());

        // Initially empty
        WorkingTreeStatusDto status1 = gitService.getStatus(repo.getId());
        assertTrue(status1.isClean());

        // Create a new file
        Path helloFile = repoPath.resolve("hello.txt");
        Files.writeString(helloFile, "Hello, ProjectSync VCS!\nSecond line.\n");

        // Status should show untracked
        WorkingTreeStatusDto status2 = gitService.getStatus(repo.getId());
        assertFalse(status2.isClean());
        assertTrue(status2.getUntracked().contains("hello.txt"));

        // Stage file
        gitService.stageFiles(repo.getId(), List.of("hello.txt"));
        WorkingTreeStatusDto status3 = gitService.getStatus(repo.getId());
        assertEquals(1, status3.getStaged().size());
        assertEquals("hello.txt", status3.getStaged().get(0).getFilePath());
        assertEquals("ADDED", status3.getStaged().get(0).getStatus());

        // Check staged diff
        DiffDto diff = gitService.getWorkingTreeDiff(repo.getId(), "hello.txt", true);
        assertNotNull(diff);
        assertTrue(diff.getPatch().contains("+Hello, ProjectSync VCS!"));
        assertEquals(2, diff.getAdditions());

        // Commit
        CommitRequest commitReq = new CommitRequest("Add hello.txt file", "Dev User", "dev@example.com");
        CommitDto commit = gitService.createCommit(repo.getId(), commitReq, "Dev User", "dev@example.com");
        assertNotNull(commit.getHash());
        assertEquals("Add hello.txt file", commit.getMessage());

        // Status after commit should be clean
        WorkingTreeStatusDto status4 = gitService.getStatus(repo.getId());
        assertTrue(status4.isClean());

        // Commit details
        CommitDetailsDto commitDetails = gitService.getCommitDetails(repo.getId(), commit.getHash());
        assertNotNull(commitDetails);
        assertEquals(1, commitDetails.getFilesChanged().size());
        assertEquals("hello.txt", commitDetails.getFilesChanged().get(0).getNewPath());
    }

    @Test
    void testBranchCreationAndSwitching() {
        CreateRepoRequest req = new CreateRepoRequest("branch-repo", "Testing branches", "main", true);
        RepositoryDetailsDto repo = gitService.createRepository(req, "brancher");

        // List initial branches
        List<BranchDto> branches = gitService.listBranches(repo.getId());
        assertEquals(1, branches.size());
        assertEquals("main", branches.get(0).getName());
        assertTrue(branches.get(0).isCurrent());

        // Create new branch
        BranchDto newBranch = gitService.createBranch(repo.getId(), new CreateBranchRequest("feature-login", null));
        assertNotNull(newBranch);
        assertEquals("feature-login", newBranch.getName());

        // Switch to new branch
        gitService.checkoutBranch(repo.getId(), "feature-login");

        // Verify active branch changed
        WorkingTreeStatusDto status = gitService.getStatus(repo.getId());
        assertEquals("feature-login", status.getCurrentBranch());

        List<BranchDto> updatedBranches = gitService.listBranches(repo.getId());
        assertEquals(2, updatedBranches.size());
        BranchDto activeBranch = updatedBranches.stream().filter(BranchDto::isCurrent).findFirst().orElseThrow();
        assertEquals("feature-login", activeBranch.getName());
    }

    @Test
    void testUnstageAndDiscard() throws IOException {
        CreateRepoRequest req = new CreateRepoRequest("unstage-repo", "Testing unstage", "main", true);
        RepositoryDetailsDto repo = gitService.createRepository(req, "tester");
        Path repoPath = Path.of(repo.getDiskPath());

        // Add a new file
        Path noteFile = repoPath.resolve("note.txt");
        Files.writeString(noteFile, "Draft note");

        // Stage
        gitService.stageFiles(repo.getId(), List.of("note.txt"));
        assertEquals(1, gitService.getStatus(repo.getId()).getStaged().size());

        // Unstage
        gitService.unstageFiles(repo.getId(), List.of("note.txt"));
        assertEquals(0, gitService.getStatus(repo.getId()).getStaged().size());
        assertTrue(gitService.getStatus(repo.getId()).getUntracked().contains("note.txt"));

        // Discard untracked
        gitService.discardChanges(repo.getId(), List.of("note.txt"));
        assertFalse(Files.exists(noteFile));
        assertTrue(gitService.getStatus(repo.getId()).isClean());
    }

    @Test
    void testRemoteConfigurationAndSyncWithBareRepo() throws Exception {
        // 1. Create a bare remote Git repository to simulate a remote server (like GitHub)
        Path bareDir = tempStorageDir.resolve("server-bare.git");
        org.eclipse.jgit.api.Git.init().setBare(true).setDirectory(bareDir.toFile()).call().close();

        // 2. Create local repository
        CreateRepoRequest req = new CreateRepoRequest("sync-repo", "Testing sync", "main", true);
        RepositoryDetailsDto repo = gitService.createRepository(req, "sync-user");

        // 3. Configure remote
        RemoteConfigDto remoteConfig = new RemoteConfigDto("origin", bareDir.toUri().toString(), null);
        gitService.setRemote(repo.getId(), remoteConfig);

        List<RemoteConfigDto> remotes = gitService.getRemotes(repo.getId());
        assertEquals(1, remotes.size());
        assertEquals("origin", remotes.get(0).getName());
        assertEquals(bareDir.toUri().toString(), remotes.get(0).getFetchUrl());

        // 4. Push to remote
        SyncRequest pushReq = new SyncRequest("origin", "main", null, null, false);
        SyncResultDto pushResult = gitService.pushRemote(repo.getId(), pushReq);
        assertTrue(pushResult.isSuccess(), "Push should succeed: " + pushResult.getMessage());

        // 5. Fetch from remote
        SyncRequest fetchReq = new SyncRequest("origin", null, null, null, false);
        SyncResultDto fetchResult = gitService.fetchRemote(repo.getId(), fetchReq);
        assertTrue(fetchResult.isSuccess(), "Fetch should succeed: " + fetchResult.getMessage());

        // 6. Pull from remote
        SyncRequest pullReq = new SyncRequest("origin", "main", null, null, false);
        SyncResultDto pullResult = gitService.pullRemote(repo.getId(), pullReq);
        assertTrue(pullResult.isSuccess(), "Pull should succeed: " + pullResult.getMessage());
    }

    @Test
    void testCloneRepository() throws Exception {
        // 1. Create a source repo with initial commit
        CreateRepoRequest srcReq = new CreateRepoRequest("source-repo", "Source for clone", "main", true);
        RepositoryDetailsDto srcRepo = gitService.createRepository(srcReq, "author");

        // 2. Clone from local source repo URI
        CloneRepoRequest cloneReq = new CloneRepoRequest();
        cloneReq.setRemoteUrl(Path.of(srcRepo.getDiskPath()).toUri().toString());
        cloneReq.setName("cloned-repo");
        cloneReq.setDescription("A successfully cloned repository");

        CloneResultDto cloneResult = gitService.cloneRepository(cloneReq, "clone-user");

        assertNotNull(cloneResult);
        assertNotNull(cloneResult.getRepository());
        assertEquals("cloned-repo", cloneResult.getRepository().getName());
        assertEquals(1, cloneResult.getRepository().getCommitCount());
        assertTrue(cloneResult.getCloneOutput().contains("Clone completed successfully"));

        // Verify disk files
        Path clonedDir = Path.of(cloneResult.getRepository().getDiskPath());
        assertTrue(Files.exists(clonedDir.resolve(".git")));
        assertTrue(Files.exists(clonedDir.resolve("README.md")));
    }
}
