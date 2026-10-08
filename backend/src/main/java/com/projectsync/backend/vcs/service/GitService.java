package com.projectsync.backend.vcs.service;

import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.vcs.dto.*;
import com.projectsync.backend.vcs.model.RepositoryMetadata;
import com.projectsync.backend.vcs.repository.RepositoryMetadataStore;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class GitService {

    private static final Pattern REPO_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");
    private final Path baseStoragePath;
    private final RepositoryMetadataStore metadataStore;

    public GitService(
            @Value("${projectsync.vcs.storage-path:${user.home}/.projectsync/repositories}") String storagePath,
            RepositoryMetadataStore metadataStore) {
        this.baseStoragePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.metadataStore = metadataStore;
        initStorageDirectory();
    }

    private void initStorageDirectory() {
        try {
            org.eclipse.jgit.storage.file.WindowCacheConfig config = new org.eclipse.jgit.storage.file.WindowCacheConfig();
            config.setPackedGitMMAP(false);
            config.install();

            if (!Files.exists(baseStoragePath)) {
                Files.createDirectories(baseStoragePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize VCS repositories directory at " + baseStoragePath, e);
        }
    }

    public Path getBaseStoragePath() {
        return baseStoragePath;
    }

    /**
     * Resolves a safe path for a new repository inside the base storage directory.
     * Prevents path traversal vulnerabilities.
     */
    private Path resolveSafePath(String repoName) {
        if (repoName == null || !REPO_NAME_PATTERN.matcher(repoName).matches()) {
            throw new BadRequestException("Invalid repository name: " + repoName);
        }
        Path target = baseStoragePath.resolve(repoName).normalize();
        if (!target.startsWith(baseStoragePath)) {
            throw new BadRequestException("Path traversal attempt detected");
        }
        return target;
    }

    private Repository openGitRepository(File dir) throws IOException {
        File gitDir = new File(dir, ".git");
        File actualDir = gitDir.exists() ? gitDir : dir;
        return new FileRepositoryBuilder()
                .setGitDir(actualDir)
                .readEnvironment()
                .findGitDir()
                .build();
    }

    private Git openGit(RepositoryMetadata meta) {
        File dir = new File(meta.getDiskPath());
        if (!dir.exists()) {
            throw new ResourceNotFoundException("Repository directory not found on disk: " + meta.getDiskPath());
        }
        try {
            Repository repository = openGitRepository(dir);
            return new Git(repository) {
                @Override
                public void close() {
                    try {
                        repository.close();
                    } finally {
                        super.close();
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to open Git repository: " + e.getMessage(), e);
        }
    }

    private RepositoryMetadata findMetadataOrThrow(String repoId) {
        return metadataStore.findById(repoId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository with ID " + repoId + " not found"));
    }

    // --------------------------------------------------------------------------
    // REPOSITORY MANAGEMENT
    // --------------------------------------------------------------------------

    public RepositoryDetailsDto createRepository(CreateRepoRequest req, String ownerUsername) {
        Path repoPath = resolveSafePath(req.getName());

        if (metadataStore.existsByName(req.getName())) {
            throw new ConflictException("A repository named '" + req.getName() + "' already exists in metadata");
        }

        if (Files.exists(repoPath) && Files.exists(repoPath.resolve(".git"))) {
            throw new ConflictException("A Git repository already exists at path: " + repoPath);
        }

        String branch = (req.getDefaultBranch() != null && !req.getDefaultBranch().isBlank())
                ? req.getDefaultBranch()
                : "main";

        try {
            Files.createDirectories(repoPath);

            Git git = Git.init()
                    .setDirectory(repoPath.toFile())
                    .setInitialBranch(branch)
                    .call();

            // Create initial commit with README.md if requested
            if (req.isInitReadme()) {
                Path readme = repoPath.resolve("README.md");
                String content = "# " + req.getName() + "\n\n"
                        + (req.getDescription() != null ? req.getDescription() : "Project repository initialized by ProjectSync-AI VCS.") + "\n";
                Files.writeString(readme, content, StandardCharsets.UTF_8);

                git.add().addFilepattern("README.md").call();

                String authorName = (ownerUsername != null && !ownerUsername.isBlank()) ? ownerUsername : "ProjectSync AI";
                String authorEmail = (ownerUsername != null && !ownerUsername.isBlank())
                        ? ownerUsername + "@projectsync.local"
                        : "support@projectsync.ai";

                git.commit()
                        .setMessage("Initial commit")
                        .setAuthor(authorName, authorEmail)
                        .setCommitter(authorName, authorEmail)
                        .call();
            }

            git.close();

            RepositoryMetadata metadata = new RepositoryMetadata(
                    null,
                    req.getName(),
                    req.getDescription(),
                    repoPath.toString(),
                    branch,
                    null,
                    ownerUsername,
                    Instant.now(),
                    Instant.now()
            );

            RepositoryMetadata saved = metadataStore.save(metadata);
            return getRepositoryDetails(saved.getId());

        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to initialize Git repository: " + e.getMessage(), e);
        }
    }

    public RepositoryDetailsDto registerExistingRepository(RegisterRepoRequest req, String ownerUsername) {
        if (metadataStore.existsByName(req.getName())) {
            throw new ConflictException("A repository named '" + req.getName() + "' is already registered");
        }

        Path target = Paths.get(req.getPath()).toAbsolutePath().normalize();
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException("Specified directory does not exist: " + target);
        }

        Path gitDir = target.resolve(".git");
        if (!Files.exists(gitDir) && !target.toString().endsWith(".git")) {
            throw new BadRequestException("Specified directory does not contain a valid .git folder: " + target);
        }

        try (Repository repo = openGitRepository(target.toFile())) {
            String branch = repo.getBranch();
            if (branch == null) branch = "main";

            StoredConfig config = repo.getConfig();
            String remoteUrl = config.getString("remote", "origin", "url");

            RepositoryMetadata metadata = new RepositoryMetadata(
                    null,
                    req.getName(),
                    req.getDescription(),
                    target.toString(),
                    branch,
                    remoteUrl,
                    ownerUsername,
                    Instant.now(),
                    Instant.now()
            );

            RepositoryMetadata saved = metadataStore.save(metadata);
            return getRepositoryDetails(saved.getId());

        } catch (IOException e) {
            throw new BadRequestException("Failed to open existing Git repository: " + e.getMessage());
        }
    }

    public CloneResultDto cloneRepository(CloneRepoRequest req, String ownerUsername) {
        String remoteUrl = req.getRemoteUrl() != null ? req.getRemoteUrl().trim() : "";
        if (remoteUrl.isBlank()) {
            throw new BadRequestException("Repository source or URL must not be blank");
        }

        // Check if remoteUrl refers to an existing internal repository by name or ID
        Optional<RepositoryMetadata> internalSource = metadataStore.findByName(remoteUrl);
        if (internalSource.isEmpty()) {
            internalSource = metadataStore.findById(remoteUrl);
        }

        String cloneUri = remoteUrl;
        String recordedRemoteUrl = remoteUrl;

        if (internalSource.isPresent()) {
            Path internalDiskPath = Paths.get(internalSource.get().getDiskPath());
            cloneUri = internalDiskPath.toUri().toString();
            recordedRemoteUrl = "internal:" + internalSource.get().getName();
        } else {
            try {
                Path localPath = Paths.get(remoteUrl);
                if (Files.exists(localPath)) {
                    cloneUri = localPath.toUri().toString();
                    recordedRemoteUrl = localPath.toString();
                }
            } catch (Exception ignored) {
            }
        }

        // Determine repository name
        String repoName = req.getName();
        if (repoName == null || repoName.trim().isBlank()) {
            if (internalSource.isPresent()) {
                repoName = internalSource.get().getName() + "-clone";
            } else {
                String cleanUrl = remoteUrl.replaceAll("/+$", "");
                if (cleanUrl.endsWith(".git")) {
                    cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 4);
                }
                int lastSlash = Math.max(cleanUrl.lastIndexOf('/'), cleanUrl.lastIndexOf('\\'));
                if (lastSlash >= 0 && lastSlash < cleanUrl.length() - 1) {
                    repoName = cleanUrl.substring(lastSlash + 1);
                } else {
                    repoName = "repo-" + System.currentTimeMillis();
                }
            }
        }
        repoName = repoName.trim();

        Path repoPath = resolveSafePath(repoName);

        if (metadataStore.existsByName(repoName)) {
            throw new ConflictException("A repository named '" + repoName + "' already exists");
        }

        if (Files.exists(repoPath)) {
            throw new ConflictException("Target directory already exists at path: " + repoPath);
        }

        StringBuilder outputLog = new StringBuilder();
        outputLog.append("Cloning into '").append(repoName).append("'...\n");
        outputLog.append("Source: ").append(internalSource.isPresent() ? "Internal Repository [" + internalSource.get().getName() + "]" : remoteUrl).append("\n");

        CloneCommand cloneCmd = Git.cloneRepository()
                .setURI(cloneUri)
                .setDirectory(repoPath.toFile())
                .setCloneAllBranches(true);

        if (req.getBranch() != null && !req.getBranch().trim().isBlank()) {
            cloneCmd.setBranch(req.getBranch().trim());
            outputLog.append("Requested branch: ").append(req.getBranch().trim()).append("\n");
        }

        if (req.getUsername() != null && !req.getUsername().isBlank()
                && req.getPersonalAccessToken() != null && !req.getPersonalAccessToken().isBlank()) {
            cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    req.getUsername().trim(), req.getPersonalAccessToken().trim()));
            outputLog.append("Credentials provided for user: ").append(req.getUsername().trim()).append("\n");
        } else if (req.getPersonalAccessToken() != null && !req.getPersonalAccessToken().isBlank()) {
            cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    req.getPersonalAccessToken().trim(), ""));
            outputLog.append("Personal Access Token credentials provided.\n");
        }

        ProgressMonitor monitor = new ProgressMonitor() {
            @Override
            public void start(int totalTasks) {
                outputLog.append("Tasks: ").append(totalTasks).append("\n");
            }

            @Override
            public void beginTask(String title, int totalWork) {
                outputLog.append("-> ").append(title);
                if (totalWork > 0) {
                    outputLog.append(" (").append(totalWork).append(" units)");
                }
                outputLog.append("\n");
            }

            @Override
            public void update(int completed) {}

            @Override
            public void endTask() {}

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void showDuration(boolean enabled) {}
        };
        cloneCmd.setProgressMonitor(monitor);

        Git git = null;
        try {
            git = cloneCmd.call();
            Repository repository = git.getRepository();
            String currentBranch = repository.getBranch();
            if (currentBranch == null) {
                currentBranch = (req.getBranch() != null && !req.getBranch().isBlank()) ? req.getBranch().trim() : "main";
            }

            ObjectId headId = repository.resolve(Constants.HEAD);
            String headSha = headId != null ? headId.getName() : "empty";
            outputLog.append("Clone completed successfully.\n");
            outputLog.append("Current branch: ").append(currentBranch).append("\n");
            outputLog.append("HEAD: ").append(headSha).append("\n");

            git.close();

            RepositoryMetadata metadata = new RepositoryMetadata(
                    null,
                    repoName,
                    (req.getDescription() != null && !req.getDescription().isBlank())
                            ? req.getDescription().trim()
                            : (internalSource.isPresent() ? "Cloned from internal repository " + internalSource.get().getName() : "Cloned from " + recordedRemoteUrl),
                    repoPath.toString(),
                    currentBranch,
                    recordedRemoteUrl,
                    ownerUsername,
                    Instant.now(),
                    Instant.now()
            );

            RepositoryMetadata saved = metadataStore.save(metadata);
            RepositoryDetailsDto details = getRepositoryDetails(saved.getId());

            return new CloneResultDto(details, "Repository cloned successfully", outputLog.toString());

        } catch (Exception e) {
            if (git != null) {
                try {
                    git.close();
                } catch (Exception ignored) {}
            }
            // Cleanup partially cloned directory
            deleteDirectoryRecursively(repoPath);

            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            throw new BadRequestException("Git clone failed: " + msg);
        }
    }

    public List<RepositorySummaryDto> listRepositories() {
        return listRepositories(null, true);
    }

    public List<RepositorySummaryDto> listRepositories(String username, boolean isFacultyOrAdmin) {
        List<RepositoryMetadata> all = metadataStore.findAll();
        List<RepositorySummaryDto> results = new ArrayList<>();

        for (RepositoryMetadata meta : all) {
            // Role-based isolation: students can only see repositories they own
            if (!isFacultyOrAdmin && username != null && !username.isBlank()) {
                String owner = meta.getOwnerUsername();
                if ("local-developer".equalsIgnoreCase(username)) {
                    if (owner != null && !owner.equalsIgnoreCase("local-developer")) {
                        continue;
                    }
                } else {
                    if (owner == null || !owner.equalsIgnoreCase(username)) {
                        continue;
                    }
                }
            }

            File dir = new File(meta.getDiskPath());
            if (!dir.exists()) {
                results.add(new RepositorySummaryDto(
                        meta.getId(),
                        meta.getName(),
                        meta.getDescription(),
                        meta.getDiskPath(),
                        meta.getDefaultBranch(),
                        0,
                        false,
                        meta.getUpdatedAt(),
                        meta.getRemoteUrl(),
                        meta.getOwnerUsername()
                ));
                continue;
            }

            try (Git git = openGit(meta)) {
                Repository repo = git.getRepository();
                String branch = repo.getBranch();
                if (branch == null) branch = meta.getDefaultBranch();

                int commitCount = 0;
                Instant lastCommitDate = meta.getUpdatedAt();

                try {
                    ObjectId head = repo.resolve(Constants.HEAD);
                    if (head != null) {
                        try (RevWalk revWalk = new RevWalk(repo)) {
                            RevCommit commit = revWalk.parseCommit(head);
                            lastCommitDate = commit.getAuthorIdent().getWhen().toInstant();
                        }
                        Iterable<RevCommit> logs = git.log().call();
                        commitCount = (int) StreamSupport.stream(logs.spliterator(), false).count();
                    }
                } catch (Exception ignored) {
                }

                Status status = git.status().call();
                boolean isClean = status.isClean();

                StoredConfig config = repo.getConfig();
                String remoteUrl = config.getString("remote", "origin", "url");

                results.add(new RepositorySummaryDto(
                        meta.getId(),
                        meta.getName(),
                        meta.getDescription(),
                        meta.getDiskPath(),
                        branch,
                        commitCount,
                        isClean,
                        lastCommitDate,
                        remoteUrl != null ? remoteUrl : meta.getRemoteUrl(),
                        meta.getOwnerUsername()
                ));
            } catch (Exception e) {
                results.add(new RepositorySummaryDto(
                        meta.getId(),
                        meta.getName(),
                        meta.getDescription(),
                        meta.getDiskPath(),
                        meta.getDefaultBranch(),
                        0,
                        false,
                        meta.getUpdatedAt(),
                        meta.getRemoteUrl(),
                        meta.getOwnerUsername()
                ));
            }
        }

        return results;
    }

    public RepositoryDetailsDto getRepositoryDetails(String repoId) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            String currentBranch = repo.getBranch();

            int commitCount = 0;
            ObjectId head = repo.resolve(Constants.HEAD);
            if (head != null) {
                try {
                    Iterable<RevCommit> logs = git.log().call();
                    commitCount = (int) StreamSupport.stream(logs.spliterator(), false).count();
                } catch (Exception ignored) {
                }
            }

            Status status = git.status().call();
            boolean isClean = status.isClean();

            List<RemoteConfigDto> remotes = getRemotes(repoId);
            String primaryRemote = remotes.isEmpty() ? meta.getRemoteUrl() : remotes.get(0).getFetchUrl();

            return new RepositoryDetailsDto(
                    meta.getId(),
                    meta.getName(),
                    meta.getDescription(),
                    meta.getDiskPath(),
                    meta.getDefaultBranch(),
                    currentBranch != null ? currentBranch : meta.getDefaultBranch(),
                    isClean,
                    commitCount,
                    primaryRemote,
                    meta.getCreatedAt(),
                    meta.getUpdatedAt(),
                    remotes,
                    meta.getOwnerUsername()
            );
        } catch (Exception e) {
            return new RepositoryDetailsDto(
                    meta.getId(),
                    meta.getName(),
                    meta.getDescription(),
                    meta.getDiskPath(),
                    meta.getDefaultBranch(),
                    meta.getDefaultBranch(),
                    false,
                    0,
                    meta.getRemoteUrl(),
                    meta.getCreatedAt(),
                    meta.getUpdatedAt(),
                    Collections.emptyList(),
                    meta.getOwnerUsername()
            );
        }
    }

    public void updateRepository(String repoId, String name, String description) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        if (name != null && !name.isBlank()) {
            if (!name.equalsIgnoreCase(meta.getName()) && metadataStore.existsByName(name)) {
                throw new ConflictException("Repository named '" + name + "' already exists");
            }
            meta.setName(name);
        }
        if (description != null) {
            meta.setDescription(description);
        }
        meta.setUpdatedAt(Instant.now());
        metadataStore.save(meta);
    }

    public void deleteRepository(String repoId, boolean deleteDiskFiles) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        metadataStore.deleteById(repoId);

        if (deleteDiskFiles) {
            Path target = Paths.get(meta.getDiskPath()).toAbsolutePath().normalize();
            // Critical security check: Only delete if directory is strictly inside baseStoragePath
            if (target.startsWith(baseStoragePath) && !target.equals(baseStoragePath)) {
                deleteDirectoryRecursively(target);
            }
        }
    }

    private void deleteDirectoryRecursively(Path path) {
        if (path != null && Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                System.err.println("Warning: Failed to recursively delete directory " + path + ": " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------------
    // WORKING TREE & STAGING
    // --------------------------------------------------------------------------

    public WorkingTreeStatusDto getStatus(String repoId) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            String currentBranch = repo.getBranch();
            Status status = git.status().call();

            List<FileStatusItem> staged = new ArrayList<>();
            for (String p : status.getAdded()) staged.add(new FileStatusItem(p, "ADDED"));
            for (String p : status.getChanged()) staged.add(new FileStatusItem(p, "MODIFIED"));
            for (String p : status.getRemoved()) staged.add(new FileStatusItem(p, "DELETED"));

            List<FileStatusItem> unstaged = new ArrayList<>();
            for (String p : status.getModified()) unstaged.add(new FileStatusItem(p, "MODIFIED"));
            for (String p : status.getMissing()) unstaged.add(new FileStatusItem(p, "DELETED"));

            List<String> untracked = new ArrayList<>(status.getUntracked());
            List<String> conflicts = new ArrayList<>(status.getConflicting());

            return new WorkingTreeStatusDto(
                    repoId,
                    currentBranch != null ? currentBranch : meta.getDefaultBranch(),
                    status.isClean(),
                    staged,
                    unstaged,
                    untracked,
                    conflicts
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get repository status: " + e.getMessage(), e);
        }
    }

    public void stageFiles(String repoId, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return;
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Status status = git.status().call();
            Set<String> missing = status.getMissing();

            for (String p : filePaths) {
                if (missing.contains(p)) {
                    git.rm().addFilepattern(p).call();
                } else {
                    git.add().addFilepattern(p).call();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to stage files: " + e.getMessage(), e);
        }
    }

    public void stageAll(String repoId) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Status status = git.status().call();
            // Stage untracked and modified
            git.add().addFilepattern(".").call();
            // Stage missing / removed files
            if (!status.getMissing().isEmpty()) {
                RmCommand rm = git.rm();
                status.getMissing().forEach(rm::addFilepattern);
                rm.call();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to stage all changes: " + e.getMessage(), e);
        }
    }

    public void unstageFiles(String repoId, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return;
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            ResetCommand reset = git.reset();
            filePaths.forEach(reset::addPath);
            reset.call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to unstage files: " + e.getMessage(), e);
        }
    }

    public void unstageAll(String repoId) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            git.reset().call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to unstage all changes: " + e.getMessage(), e);
        }
    }

    public void discardChanges(String repoId, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return;
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Status status = git.status().call();
            Set<String> untracked = status.getUntracked();

            List<String> trackedToCheckout = new ArrayList<>();
            Path repoRoot = Paths.get(meta.getDiskPath());

            for (String p : filePaths) {
                if (untracked.contains(p)) {
                    Path file = repoRoot.resolve(p).normalize();
                    if (file.startsWith(repoRoot)) {
                        Files.deleteIfExists(file);
                    }
                } else {
                    trackedToCheckout.add(p);
                }
            }

            if (!trackedToCheckout.isEmpty()) {
                CheckoutCommand checkout = git.checkout();
                trackedToCheckout.forEach(checkout::addPath);
                checkout.call();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to discard changes: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------------------------
    // COMMITS & HISTORY
    // --------------------------------------------------------------------------

    public CommitDto createCommit(String repoId, CommitRequest req, String fallbackAuthor, String fallbackEmail) {
        if (req.getMessage() == null || req.getMessage().trim().isEmpty()) {
            throw new BadRequestException("Commit message cannot be empty");
        }

        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Status status = git.status().call();
            if (status.getAdded().isEmpty() && status.getChanged().isEmpty() && status.getRemoved().isEmpty()) {
                throw new BadRequestException("No staged changes to commit. Stage files first before committing.");
            }

            String authorName = (req.getAuthorName() != null && !req.getAuthorName().isBlank())
                    ? req.getAuthorName()
                    : ((fallbackAuthor != null && !fallbackAuthor.isBlank()) ? fallbackAuthor : "ProjectSync Developer");

            String authorEmail = (req.getAuthorEmail() != null && !req.getAuthorEmail().isBlank())
                    ? req.getAuthorEmail()
                    : ((fallbackEmail != null && !fallbackEmail.isBlank()) ? fallbackEmail : "developer@projectsync.local");

            RevCommit revCommit = git.commit()
                    .setMessage(req.getMessage().trim())
                    .setAuthor(authorName, authorEmail)
                    .setCommitter(authorName, authorEmail)
                    .call();

            List<String> parents = Arrays.stream(revCommit.getParents())
                    .map(AnyObjectId::getName)
                    .collect(Collectors.toList());

            meta.setUpdatedAt(Instant.now());
            metadataStore.save(meta);

            return new CommitDto(
                    revCommit.getName(),
                    revCommit.getName().substring(0, Math.min(7, revCommit.getName().length())),
                    revCommit.getFullMessage(),
                    revCommit.getAuthorIdent().getName(),
                    revCommit.getAuthorIdent().getEmailAddress(),
                    revCommit.getAuthorIdent().getWhen().toInstant(),
                    parents
            );
        } catch (Exception e) {
            throw new RuntimeException("Commit failed: " + e.getMessage(), e);
        }
    }

    public List<CommitDto> getCommitHistory(String repoId, String branch, int limit) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            ObjectId head = (branch != null && !branch.isBlank()) ? repo.resolve(branch) : repo.resolve(Constants.HEAD);

            if (head == null) {
                return Collections.emptyList();
            }

            LogCommand log = git.log().add(head);
            if (limit > 0) log.setMaxCount(limit);

            Iterable<RevCommit> commits = log.call();
            List<CommitDto> result = new ArrayList<>();

            for (RevCommit c : commits) {
                List<String> parents = Arrays.stream(c.getParents())
                        .map(AnyObjectId::getName)
                        .collect(Collectors.toList());

                result.add(new CommitDto(
                        c.getName(),
                        c.getName().substring(0, Math.min(7, c.getName().length())),
                        c.getFullMessage(),
                        c.getAuthorIdent().getName(),
                        c.getAuthorIdent().getEmailAddress(),
                        c.getAuthorIdent().getWhen().toInstant(),
                        parents
                ));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public CommitDetailsDto getCommitDetails(String repoId, String commitSha) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            ObjectId objId = repo.resolve(commitSha);
            if (objId == null) {
                throw new ResourceNotFoundException("Commit not found: " + commitSha);
            }

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(objId);

                List<String> parentHashes = Arrays.stream(commit.getParents())
                        .map(AnyObjectId::getName)
                        .collect(Collectors.toList());

                CommitDto commitDto = new CommitDto(
                        commit.getName(),
                        commit.getName().substring(0, Math.min(7, commit.getName().length())),
                        commit.getFullMessage(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        commit.getAuthorIdent().getWhen().toInstant(),
                        parentHashes
                );

                List<CommitFileDiffDto> filesChanged = new ArrayList<>();
                int totalAdditions = 0;
                int totalDeletions = 0;

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter df = new DiffFormatter(out)) {
                    df.setRepository(repo);
                    df.setDiffComparator(RawTextComparator.DEFAULT);
                    df.setDetectRenames(true);

                    AbstractTreeIterator oldTreeIter;
                    if (commit.getParentCount() > 0) {
                        RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                        oldTreeIter = getCanonicalTreeParser(repo, parent.getTree());
                    } else {
                        oldTreeIter = new EmptyTreeIterator();
                    }

                    AbstractTreeIterator newTreeIter = getCanonicalTreeParser(repo, commit.getTree());
                    List<DiffEntry> diffs = df.scan(oldTreeIter, newTreeIter);

                    for (DiffEntry diff : diffs) {
                        out.reset();
                        df.format(diff);
                        String patch = out.toString(StandardCharsets.UTF_8);

                        int additions = 0;
                        int deletions = 0;
                        for (String line : patch.split("\n")) {
                            if (line.startsWith("+") && !line.startsWith("+++")) additions++;
                            else if (line.startsWith("-") && !line.startsWith("---")) deletions++;
                        }
                        totalAdditions += additions;
                        totalDeletions += deletions;

                        filesChanged.add(new CommitFileDiffDto(
                                diff.getChangeType().name(),
                                diff.getOldPath(),
                                diff.getNewPath(),
                                additions,
                                deletions,
                                patch
                        ));
                    }
                }

                return new CommitDetailsDto(
                        commitDto,
                        parentHashes,
                        filesChanged,
                        totalAdditions,
                        totalDeletions
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get commit details: " + e.getMessage(), e);
        }
    }

    private CanonicalTreeParser getCanonicalTreeParser(Repository repo, RevTree tree) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        try (ObjectReader reader = repo.newObjectReader()) {
            parser.reset(reader, tree.getId());
        }
        return parser;
    }

    // --------------------------------------------------------------------------
    // BRANCH MANAGEMENT
    // --------------------------------------------------------------------------

    public List<BranchDto> listBranches(String repoId) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            String currentBranch = repo.getBranch();

            List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            List<BranchDto> result = new ArrayList<>();

            try (RevWalk revWalk = new RevWalk(repo)) {
                for (Ref ref : refs) {
                    String name = Repository.shortenRefName(ref.getName());
                    boolean isCurrent = name.equals(currentBranch);
                    String headSha = "";
                    String headMsg = "";

                    try {
                        ObjectId target = ref.getObjectId();
                        if (target != null) {
                            RevCommit commit = revWalk.parseCommit(target);
                            headSha = commit.getName().substring(0, Math.min(7, commit.getName().length()));
                            headMsg = commit.getShortMessage();
                        }
                    } catch (Exception ignored) {
                    }

                    result.add(new BranchDto(name, isCurrent, headSha, headMsg));
                }
            }

            if (result.isEmpty() && currentBranch != null) {
                result.add(new BranchDto(currentBranch, true, "", ""));
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list branches: " + e.getMessage(), e);
        }
    }

    public BranchDto createBranch(String repoId, CreateBranchRequest req) {
        String name = req.getBranchName();
        if (name == null || !Repository.isValidRefName(Constants.R_HEADS + name)) {
            throw new BadRequestException("Invalid Git branch name: " + name);
        }

        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            CreateBranchCommand cmd = git.branchCreate().setName(name);
            if (req.getStartPoint() != null && !req.getStartPoint().isBlank()) {
                cmd.setStartPoint(req.getStartPoint());
            }
            Ref ref = cmd.call();
            return new BranchDto(name, false, ref.getObjectId().getName().substring(0, 7), "");
        } catch (Exception e) {
            throw new BadRequestException("Failed to create branch: " + e.getMessage());
        }
    }

    public void checkoutBranch(String repoId, String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new BadRequestException("Branch name is required for checkout");
        }
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            git.checkout().setName(branchName).call();
        } catch (Exception e) {
            throw new BadRequestException("Failed to checkout branch " + branchName + ": " + e.getMessage());
        }
    }

    public void deleteBranch(String repoId, String branchName, boolean force) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            if (branchName.equals(repo.getBranch())) {
                throw new BadRequestException("Cannot delete the currently active branch: " + branchName);
            }
            git.branchDelete().setBranchNames(branchName).setForce(force).call();
        } catch (Exception e) {
            throw new BadRequestException("Failed to delete branch " + branchName + ": " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    // DIFF VIEWER
    // --------------------------------------------------------------------------

    public DiffDto getWorkingTreeDiff(String repoId, String filePath, boolean staged) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try (DiffFormatter df = new DiffFormatter(out)) {
                df.setRepository(repo);
                df.setDiffComparator(RawTextComparator.DEFAULT);
                df.setDetectRenames(true);

                if (filePath != null && !filePath.isBlank()) {
                    df.setPathFilter(PathFilter.create(filePath));
                }

                List<DiffEntry> diffs;
                ObjectId head = repo.resolve(Constants.HEAD);

                if (staged) {
                    // Staged diff: compare HEAD tree with Index
                    AbstractTreeIterator oldTree = (head != null)
                            ? getCanonicalTreeParser(repo, new RevWalk(repo).parseCommit(head).getTree())
                            : new EmptyTreeIterator();
                    diffs = df.scan(oldTree, new org.eclipse.jgit.dircache.DirCacheIterator(repo.readDirCache()));
                } else {
                    // Unstaged diff: compare Index with Working Tree
                    diffs = df.scan(new org.eclipse.jgit.dircache.DirCacheIterator(repo.readDirCache()),
                            new org.eclipse.jgit.treewalk.FileTreeIterator(repo));
                }

                for (DiffEntry diff : diffs) {
                    df.format(diff);
                }

                String patch = out.toString(StandardCharsets.UTF_8);
                int additions = 0;
                int deletions = 0;

                for (String line : patch.split("\n")) {
                    if (line.startsWith("+") && !line.startsWith("+++")) additions++;
                    else if (line.startsWith("-") && !line.startsWith("---")) deletions++;
                }

                return new DiffDto(filePath, staged, patch, additions, deletions);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute diff: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------------------------
    // REMOTES & GITHUB SYNCHRONIZATION
    // --------------------------------------------------------------------------

    public List<RemoteConfigDto> getRemotes(String repoId) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            StoredConfig config = git.getRepository().getConfig();
            Set<String> remoteNames = config.getSubsections("remote");
            List<RemoteConfigDto> result = new ArrayList<>();

            for (String rName : remoteNames) {
                String fetchUrl = config.getString("remote", rName, "url");
                String pushUrl = config.getString("remote", rName, "pushurl");
                result.add(new RemoteConfigDto(rName, fetchUrl, pushUrl != null ? pushUrl : fetchUrl));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void setRemote(String repoId, RemoteConfigDto req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BadRequestException("Remote name is required");
        }
        if (req.getFetchUrl() == null || req.getFetchUrl().isBlank()) {
            throw new BadRequestException("Remote fetch URL is required");
        }

        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            Repository repo = git.getRepository();
            StoredConfig config = repo.getConfig();

            config.setString("remote", req.getName(), "url", req.getFetchUrl());
            config.setString("remote", req.getName(), "fetch", "+refs/heads/*:refs/remotes/" + req.getName() + "/*");
            if (req.getPushUrl() != null && !req.getPushUrl().isBlank()) {
                config.setString("remote", req.getName(), "pushurl", req.getPushUrl());
            } else {
                config.unset("remote", req.getName(), "pushurl");
            }
            config.save();

            if ("origin".equalsIgnoreCase(req.getName())) {
                meta.setRemoteUrl(req.getFetchUrl());
                meta.setUpdatedAt(Instant.now());
                metadataStore.save(meta);
            }
        } catch (Exception e) {
            throw new BadRequestException("Failed to configure remote: " + e.getMessage());
        }
    }

    public SyncResultDto fetchRemote(String repoId, SyncRequest req) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            FetchCommand fetch = git.fetch().setRemote(req.getRemoteName());
            if (req.getUsername() != null && req.getPersonalAccessToken() != null) {
                fetch.setCredentialsProvider(new UsernamePasswordCredentialsProvider(req.getUsername(), req.getPersonalAccessToken()));
            }

            FetchResult result = fetch.call();
            StringBuilder details = new StringBuilder();
            for (TrackingRefUpdate update : result.getTrackingRefUpdates()) {
                details.append(update.getLocalName()).append(" -> ").append(update.getResult().name()).append("\n");
            }

            return new SyncResultDto(true, "FETCH", "Fetched successfully from " + req.getRemoteName(), details.toString());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Nothing to fetch")) {
                return new SyncResultDto(true, "FETCH", "Already up to date (nothing to fetch)", "");
            }
            return new SyncResultDto(false, "FETCH", "Fetch failed: " + e.getMessage(), e.toString());
        }
    }

    public SyncResultDto pullRemote(String repoId, SyncRequest req) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            PullCommand pull = git.pull().setRemote(req.getRemoteName());
            if (req.getBranch() != null && !req.getBranch().isBlank()) {
                pull.setRemoteBranchName(req.getBranch());
            }
            if (req.getUsername() != null && req.getPersonalAccessToken() != null) {
                pull.setCredentialsProvider(new UsernamePasswordCredentialsProvider(req.getUsername(), req.getPersonalAccessToken()));
            }

            PullResult result = pull.call();
            boolean success = result.isSuccessful();
            String mergeStatus = (result.getMergeResult() != null) ? result.getMergeResult().getMergeStatus().name() : "OK";

            return new SyncResultDto(
                    success,
                    "PULL",
                    success ? "Pull completed (" + mergeStatus + ")" : "Pull finished with status: " + mergeStatus,
                    "Merge Status: " + mergeStatus
            );
        } catch (Exception e) {
            return new SyncResultDto(false, "PULL", "Pull failed: " + e.getMessage(), e.toString());
        }
    }

    public SyncResultDto pushRemote(String repoId, SyncRequest req) {
        RepositoryMetadata meta = findMetadataOrThrow(repoId);
        try (Git git = openGit(meta)) {
            PushCommand push = git.push().setRemote(req.getRemoteName()).setForce(req.isForce());
            if (req.getUsername() != null && req.getPersonalAccessToken() != null) {
                push.setCredentialsProvider(new UsernamePasswordCredentialsProvider(req.getUsername(), req.getPersonalAccessToken()));
            }

            Iterable<PushResult> results = push.call();
            StringBuilder details = new StringBuilder();
            boolean allOk = true;

            for (PushResult pr : results) {
                for (RemoteRefUpdate update : pr.getRemoteUpdates()) {
                    details.append(update.getSrcRef()).append(" -> ").append(update.getStatus().name()).append("\n");
                    if (update.getStatus() != RemoteRefUpdate.Status.OK && update.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE) {
                        allOk = false;
                    }
                }
            }

            return new SyncResultDto(
                    allOk,
                    "PUSH",
                    allOk ? "Pushed successfully to " + req.getRemoteName() : "Push encountered errors",
                    details.toString()
            );
        } catch (Exception e) {
            return new SyncResultDto(false, "PUSH", "Push failed: " + e.getMessage(), e.toString());
        }
    }
}
