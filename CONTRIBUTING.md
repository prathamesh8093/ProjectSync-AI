# ProjectSync AI — Contributing & Collaboration Guide

Welcome to the team! This document outlines our team's Git workflow, branch conventions, code review process, and safety precautions.

---

## 1. Quick Developer Setup

1. **Clone the Repository**:
   ```powershell
   git clone https://github.com/prathamesh8093/ProjectSync-AI.git
   cd ProjectSync-AI
   ```
2. **Switch to the Active Development Branch**:
   ```powershell
   git checkout feature/project-upload
   ```
3. **Install Dependencies**:
   - Backend: Runs automatically via Maven Wrapper (`.\mvnw.cmd compile`).
   - Frontend:
     ```powershell
     cd frontend
     npm install
     ```

---

## 2. Branching Strategy

Our team uses a standard Git feature-branch workflow.

```text
main (Protected — Production / Evaluated Milestones)
  │
  └── feature/project-upload (Shared Integration Branch)
        │
        ├── feature/alex-ai-feedback
        └── fix/sarah-cors-preflight
```

### Golden Rules
- **NEVER push directly to `main`**: Direct commits to `main` are restricted.
- **Always branch off the latest integration branch**:
  ```powershell
  # Ensure you have latest upstream changes
  git checkout feature/project-upload
  git pull origin feature/project-upload

  # Create your feature branch
  git checkout -b feature/<your-name>-<short-description>
  ```
- **Branch Naming Conventions**:
  - `feature/<name>-<description>`: For new features (e.g., `feature/alex-task-filters`).
  - `fix/<name>-<description>`: For bug fixes (e.g., `fix/sarah-login-redirect`).
  - `docs/<name>-<description>`: For documentation updates (e.g., `docs/john-api-guide`).

---

## 3. Commit Message Conventions

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

| Prefix | Description | Example |
|---|---|---|
| `feat:` | A new user-facing feature | `feat: implement student assessment submission` |
| `fix:` | A bug fix | `fix: resolve Hikari pool timeout on Supabase connection` |
| `docs:` | Documentation changes only | `docs: add troubleshooting steps for Windows PowerShell` |
| `refactor:` | Code restructuring without changing behavior | `refactor: extract Gemini API client into dedicated service` |
| `test:` | Adding or fixing automated tests | `test: add unit tests for GitService staging and commit` |
| `chore:` | Build configuration or dependency updates | `chore: update Vite proxy configuration for port 8081` |

---

## 4. Keeping Your Branch Updated & Handling Conflicts

Before submitting your work, sync your branch with upstream updates to ensure there are no merge conflicts:

```powershell
# Commit your current work first
git add .
git commit -m "feat: complete milestone task"

# Fetch latest changes from remote
git fetch origin

# Rebase or merge integration branch into your feature branch
git merge origin/feature/project-upload
```

If conflicts arise:
1. Open the conflicting files and resolve the marked conflicts (`<<<<<<<`, `=======`, `>>>>>>>`).
2. Test the build:
   - Backend: `cd backend; .\mvnw.cmd test`
   - Frontend: `cd frontend; npm run build`
3. Stage resolved files:
   ```powershell
   git add <resolved-file>
   git commit -m "merge: resolve conflicts with feature/project-upload"
   ```

---

## 5. Opening a Pull Request (PR)

1. **Push your branch to GitHub**:
   ```powershell
   git push -u origin feature/<your-name>-<description>
   ```
2. Navigate to the GitHub repository:
   [https://github.com/prathamesh8093/ProjectSync-AI](https://github.com/prathamesh8093/ProjectSync-AI)
3. Click **Compare & pull request**.
4. Set the **base** branch:
   - Base branch: `feature/project-upload` (or `main` when integrating major project milestones).
   - Compare branch: `feature/<your-name>-<description>`.
5. Provide a clear PR description detailing:
   - What changes were made.
   - Verification steps taken.
   - Any database migrations or new environment variables needed.
6. Request at least one teammate review before merging.

---

## 6. Safety & Security Rules

> [!CAUTION]
> **Strict Security Protocols for All Team Collaborators:**
> 1. **Zero Secret Policy**: Never commit `.env` files, passwords, private keys, database URLs containing credentials, or API keys (`GEMINI_API_KEY`, `SUPABASE_SERVICE_ROLE_KEY`).
> 2. **Shared Database Hygiene**: Never execute `DROP TABLE`, `ALTER TABLE ... DROP COLUMN`, or destructive DDL on the shared Supabase database.
> 3. **Run Pre-Commit Checks**: Always ensure the following two commands pass before pushing:
>    - Backend: `cd backend; .\mvnw.cmd test`
>    - Frontend: `cd frontend; npm run build`
