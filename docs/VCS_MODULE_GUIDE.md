# ProjectSync-AI: Version Control System (VCS) Module Documentation

## 1. Overview & Architecture

The **ProjectSync-AI VCS Module** is a full-featured, real Git-based version control system built directly into ProjectSync-AI. It enables students and faculty to manage real Git repositories on the local filesystem, perform atomic commits, inspect visual unified diffs, branch, and synchronize with personal GitHub accounts.

### High-Level Architecture

```
[ React + Vite Frontend (Port 5173) ]
  └── VcsDashboard (/student/vcs)
        ├── Repository Manager
        ├── Working Tree & Staging
        ├── Unified Diff Viewer
        ├── Commit Composer & History Log
        ├── Branch Selector & Manager
        └── GitHub Remote Synchronizer (Fetch / Pull / Push)
              │ (REST APIs over HTTP)
              ▼
[ Spring Boot Backend (Port 8081) ]
  └── VcsController (/api/vcs/**)
        ├── GitService (Core Engine powered by Eclipse JGit 7.1.0)
        │     └── Operates on physical .git repositories in ~/.projectsync/repositories/
        │
        └── RepositoryMetadataStore (Storage Abstraction)
              ├── LocalRepositoryMetadataStore (Active default: persists to metadata.json)
              └── [Future] SupabaseRepositoryMetadataStore
```

---

## 2. Standalone Local Development Mode (Without Supabase)

Because team Supabase projects can be paused or access may be pending, the backend includes an **explicit local profile**:

* **Profile `local` (Default):**
  Uses an embedded **H2 Database** configured in PostgreSQL compatibility mode (`MODE=PostgreSQL`). All JPA entities, repositories, and security beans initialize cleanly without needing network connectivity or remote Supabase credentials.
* **Profile `supabase` (Production / Team DB):**
  Preserves the full PostgreSQL connection string and Supabase storage configurations. To activate:
  Set `SPRING_PROFILES_ACTIVE=supabase` in `backend/.env` or environment variables once your Supabase project is active and restored.

---

## 3. Storage & Path-Traversal Security

* All managed repositories are isolated outside the project codebase in:
  `~/.projectsync/repositories/` (configurable via `PROJECTSYNC_VCS_PATH`).
* **Path Traversal Protection:**
  Every repository creation, registration, and filesystem operation is validated with strict regex patterns (`^[a-zA-Z0-9_.-]+$`) and canonical path boundary checks. Any attempt to use `../` or escape the storage directory is blocked immediately with a `400 Bad Request`.
* **Safe Unregister vs. Delete:**
  Unregistering a repository removes its metadata while keeping disk files safe. Disk deletion requires an explicit parameter and is strictly confined to the safe storage root.

---

## 4. Endpoints Reference (`/api/vcs`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/vcs/repositories` | List all registered repositories |
| `POST` | `/api/vcs/repositories` | Initialize new Git repository on disk |
| `POST` | `/api/vcs/repositories/register` | Register an existing local directory with `.git` |
| `GET` | `/api/vcs/repositories/{id}` | Get repository details, branch, and status |
| `PUT` | `/api/vcs/repositories/{id}` | Update repository name and description |
| `DELETE` | `/api/vcs/repositories/{id}?deleteFiles=false` | Unregister repository (or delete disk files) |
| `GET` | `/api/vcs/repositories/{id}/status` | Get staged, unstaged, untracked, and clean status |
| `POST` | `/api/vcs/repositories/{id}/stage` | Stage specific files (`{ filePaths: [...] }`) |
| `POST` | `/api/vcs/repositories/{id}/stage-all` | Stage all working tree changes |
| `POST` | `/api/vcs/repositories/{id}/unstage` | Unstage specific files |
| `POST` | `/api/vcs/repositories/{id}/unstage-all` | Unstage all files back to working tree |
| `POST` | `/api/vcs/repositories/{id}/discard` | Discard changes to specified files |
| `POST` | `/api/vcs/repositories/{id}/commit` | Commit staged changes (`{ message, authorName, authorEmail }`) |
| `GET` | `/api/vcs/repositories/{id}/commits` | Get commit history with authors, timestamps, and SHAs |
| `GET` | `/api/vcs/repositories/{id}/commits/{sha}` | Get commit details with per-file additions/deletions diffs |
| `GET` | `/api/vcs/repositories/{id}/branches` | List all local and remote branches |
| `POST` | `/api/vcs/repositories/{id}/branches` | Create a new Git branch |
| `POST` | `/api/vcs/repositories/{id}/branches/checkout` | Switch / checkout active branch |
| `DELETE` | `/api/vcs/repositories/{id}/branches/{name}` | Delete a branch |
| `GET` | `/api/vcs/repositories/{id}/diff` | Get unified diff text for a file or working tree |
| `GET` | `/api/vcs/repositories/{id}/remotes` | List configured remotes |
| `POST` | `/api/vcs/repositories/{id}/remotes` | Configure remote repository URL (`origin`) |
| `POST` | `/api/vcs/repositories/{id}/fetch` | Fetch remote commits and branches |
| `POST` | `/api/vcs/repositories/{id}/pull` | Pull and merge remote branch into active branch |
| `POST` | `/api/vcs/repositories/{id}/push` | Push local commits to remote branch |

---

## 5. Personal GitHub Account Integration

To synchronize with your personal GitHub account:

1. **Create a Personal Access Token (PAT) on GitHub:**
   - Go to GitHub -> Settings -> Developer Settings -> Personal access tokens -> Tokens (classic).
   - Generate token with the `repo` scope.
2. **Configure Remote in the VCS Dashboard:**
   - Click **"GitHub & Remotes"** -> **"Configure Remote URL"**.
   - Enter your personal repo URL (e.g. `https://github.com/your-username/my-project.git`).
3. **Execute Sync (Fetch, Pull, or Push):**
   - Click **"Push"** or **"Pull"**.
   - Enter your GitHub username and PAT in the secure modal.
   - *Security Guarantee:* Tokens are processed strictly in-memory during the request. They are **never** written to disk, saved in repository metadata, or logged.

---

## 6. How to Run Locally

### Start Backend (Spring Boot):
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Tomcat starts on port **8081** with context `/api`.

### Start Frontend (React + Vite):
```powershell
cd frontend
npm run dev
```
Vite runs on **http://localhost:5173**. Open your browser and navigate to `/student/vcs` or click **"Version Control (VCS)"** on the Student Dashboard.
