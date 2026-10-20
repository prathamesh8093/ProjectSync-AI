# ProjectSync AI — Complete REST API Reference

All endpoints are hosted at `http://localhost:8081` with base path `/api`.

Unless marked **Public**, all endpoints require a Bearer token in the `Authorization` header:
```http
Authorization: Bearer <your-jwt-token>
```

---

## 1. Authentication Endpoints (`/api/auth`)

### Register New User
- **Method**: `POST`
- **URL**: `/api/auth/register`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "alex.student@university.edu",
    "password": "Password123!",
    "fullName": "Alex Rivera",
    "role": "STUDENT"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "email": "alex.student@university.edu",
    "fullName": "Alex Rivera",
    "role": "STUDENT"
  }
  ```

### User Login
- **Method**: `POST`
- **URL**: `/api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "alex.student@university.edu",
    "password": "Password123!"
  }
  ```
- **Response**: `200 OK` with JWT token.

---

## 2. Profiles (`/api/student-profile`, `/api/faculty-profile`)

### Get Student Profile
- **Method**: `GET`
- **URL**: `/api/student-profile`
- **Role**: `STUDENT`
- **Response**: `200 OK`
  ```json
  {
    "studentId": "STU-2026-001",
    "department": "Computer Science",
    "year": "FINAL_YEAR",
    "bio": "Full-stack developer interested in distributed systems."
  }
  ```

### Update Student Profile
- **Method**: `PUT`
- **URL**: `/api/student-profile`
- **Role**: `STUDENT`
- **Request Body**: Same fields as Get Student Profile.

### Get / Update Faculty Profile
- **Method**: `GET` | `PUT`
- **URL**: `/api/faculty-profile`
- **Role**: `FACULTY`
- **Fields**: `department`, `designation`, `researchAreas`.

---

## 3. Team Management (`/api/teams`)

### Create Team
- **Method**: `POST`
- **URL**: `/api/teams`
- **Role**: `STUDENT`
- **Request Body**:
  ```json
  {
    "name": "Team Polaris"
  }
  ```
- **Response**: `201 Created`

### Get My Team
- **Method**: `GET`
- **URL**: `/api/teams/my-team`
- **Role**: `STUDENT`

### Add Team Member
- **Method**: `POST`
- **URL**: `/api/teams/{teamId}/members`
- **Role**: `STUDENT` (Team Leader only)
- **Request Body**:
  ```json
  {
    "studentEmail": "teammate@university.edu"
  }
  ```

---

## 4. Project Proposals & AI Drafting (`/api/project-proposals`)

### Submit Project Proposal
- **Method**: `POST`
- **URL**: `/api/project-proposals`
- **Role**: `STUDENT` (Team Leader)
- **Request Body**:
  ```json
  {
    "title": "Autonomous Drone Fleet Management",
    "abstractText": "Decentralized scheduling system for logistics drones...",
    "techStack": "Java, Spring Boot, React, PostgreSQL",
    "teamId": "d3b07384-d113-40e1-a08e-5b1234567890"
  }
  ```

### Get My Team's Proposal
- **Method**: `GET`
- **URL**: `/api/project-proposals/my-proposal`
- **Role**: `STUDENT`

### AI Proposal Draft (Gemini AI)
- **Method**: `POST`
- **URL**: `/api/project-proposals/ai-draft`
- **Role**: `STUDENT`
- **Request Body**:
  ```json
  {
    "topic": "Smart Campus Energy Optimization",
    "domain": "IoT and Sustainability",
    "preferredTech": "Python, React, MQTT"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "suggestedTitle": "SmartGrid Campus: Real-Time IoT Energy Monitoring",
    "abstractDraft": "This project implements an IoT telemetry pipeline...",
    "suggestedTechStack": "React, Spring Boot, TimescaleDB, MQTT",
    "keyModules": ["Sensor Telemetry", "Anomaly Detection", "Analytics Dashboard"]
  }
  ```

---

## 5. Faculty Review (`/api/faculty/proposals`)

### Get All Proposals Pending Review
- **Method**: `GET`
- **URL**: `/api/faculty/proposals`
- **Role**: `FACULTY`

### Submit Proposal Review Decision
- **Method**: `PUT`
- **URL**: `/api/faculty/proposals/{proposalId}/review`
- **Role**: `FACULTY`
- **Request Body**:
  ```json
  {
    "status": "APPROVED",
    "feedback": "Strong proposal. Proceed to sprint planning."
  }
  ```
- *Note*: Approving a proposal automatically initializes the active `Project` record with code `PRJ-YYYY-XXX`.

---

## 6. Agile Workspace: Sprints & Tasks

### Sprints (`/api/projects/{projectId}/sprints`)
- `POST /api/projects/{projectId}/sprints`: Create sprint (`name`, `goal`, `startDate`, `endDate`).
- `GET /api/projects/{projectId}/sprints`: List all sprints for the project.
- `PATCH /api/projects/{projectId}/sprints/{sprintId}/status`: Update status (`PLANNED`, `ACTIVE`, `COMPLETED`).
- `POST /api/projects/{projectId}/sprints/{sprintId}/ai-breakdown`: **Gemini AI** automated sprint task generation.

### Tasks (`/api/projects/{projectId}/tasks`)
- `POST /api/projects/{projectId}/tasks`: Create task (`title`, `description`, `priority`, `sprintId`, `assignedToUserId`).
- `GET /api/projects/{projectId}/tasks`: List all tasks (supports filtering by sprint).
- `PATCH /api/projects/{projectId}/tasks/{taskId}/status`: Update status (`TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`).
- `PATCH /api/projects/{projectId}/tasks/{taskId}/assignee`: Reassign task to another team member.
- `DELETE /api/projects/{projectId}/tasks/{taskId}`: Delete task (Leader or creator only).

### Task Comments & Audit History
- `POST /api/projects/{projectId}/tasks/{taskId}/comments`: Add discussion comment.
- `GET /api/projects/{projectId}/tasks/{taskId}/comments`: List comments.
- `GET /api/projects/{projectId}/tasks/{taskId}/history`: View audit log of changes made to task.

---

## 7. Project Documents & File Storage

### Collaborative Documents (`/api/projects/{projectId}/documents`)
- `POST /api/projects/{projectId}/documents`: Create text document/spec.
- `GET /api/projects/{projectId}/documents`: List documents.
- `PUT /api/projects/{projectId}/documents/{docId}`: Update document.
- `DELETE /api/projects/{projectId}/documents/{docId}`: Delete document.

### Supabase File Storage (`/api/projects/{projectId}/files`)
- `POST /api/projects/{projectId}/files`: Upload binary attachment (`multipart/form-data`, file key `file`).
- `GET /api/projects/{projectId}/files`: List metadata for all files in project.
- `GET /api/projects/{projectId}/files/{fileId}/download`: Stream file bytes directly from Supabase Storage.
- `DELETE /api/projects/{projectId}/files/{fileId}`: Remove file.

---

## 8. Faculty Analytics & AI Governance (`/api/faculty/analytics/projects`)

### Get Real-Time Project Analytics
- **Method**: `GET`
- **URL**: `/api/faculty/analytics/projects/{projectId}/analytics`
- **Role**: `FACULTY`
- **Response**: Aggregated sprint velocity, task completion percentages, and member workload distributions.

### AI Code Quality Inspection
- **Method**: `GET`
- **URL**: `/api/faculty/analytics/projects/{projectId}/code-inspection`
- **Role**: `FACULTY`
- **Response**: `AiCodeQualityInspectionResponse` containing code complexity metrics, security audit alerts, and architectural recommendations.

### AI Milestone Delay Risk Prediction
- **Method**: `GET`
- **URL**: `/api/faculty/analytics/projects/{projectId}/delay-risk-prediction`
- **Role**: `FACULTY`
- **Response**: Risk probability score (`LOW`, `MODERATE`, `HIGH`) with bottleneck task indicators.

### AI Rubric Evaluation
- **Method**: `GET`
- **URL**: `/api/faculty/analytics/projects/{projectId}/rubric-evaluation`
- **Role**: `FACULTY`
- **Response**: Rubric breakdown across Architecture, Execution, Testing, and Documentation.

### Export Comprehensive Report
- **Method**: `GET`
- **URL**: `/api/faculty/analytics/projects/{projectId}/export-report`
- **Produces**: `text/html` (Single-page printable evaluation report).

---

## 9. Version Control System (VCS) (`/api/vcs`)

### VCS Health & Diagnostic
- **Method**: `GET`
- **URL**: `/api/vcs/health`
- **Access**: Public
- **Response**:
  ```json
  {
    "status": "UP",
    "storageMode": "local",
    "baseStoragePath": "C:\\Users\\...\\.projectsync\\repositories",
    "activeRepositories": 3
  }
  ```

### Repository Lifecycle
- `GET /api/vcs/repositories`: List repositories owned by or assigned to current user.
- `POST /api/vcs/repositories`: Create and initialize a new Git repository (`name`, `description`, `initReadme`).
- `POST /api/vcs/repositories/clone`: Clone existing repository into an isolated project workspace.
- `GET /api/vcs/repositories/{id}`: Detailed repository summary.
- `DELETE /api/vcs/repositories/{id}`: Delete local repository.

### Working Tree Operations
- `GET /api/vcs/repositories/{id}/status`: Working tree status (`stagedFiles`, `unstagedFiles`, `untrackedFiles`, `currentBranch`).
- `POST /api/vcs/repositories/{id}/stage`: Stage specific file paths (`{"files": ["src/App.jsx"]}`).
- `POST /api/vcs/repositories/{id}/stage-all`: Stage all modified and untracked files.
- `POST /api/vcs/repositories/{id}/unstage`: Unstage file paths.
- `POST /api/vcs/repositories/{id}/discard`: Discard local uncommitted modifications.
- `POST /api/vcs/repositories/{id}/commit`: Commit staged changes (`{"message": "feat: add user profile"}`).

### Commits & Branches
- `GET /api/vcs/repositories/{id}/commits`: Paginated Git commit log.
- `GET /api/vcs/repositories/{id}/commits/{sha}`: Commit detail and changed file diffs.
- `GET /api/vcs/repositories/{id}/branches`: List all local and remote branches.
- `POST /api/vcs/repositories/{id}/branches`: Create new branch (`{"name": "feature/login"}`).
- `POST /api/vcs/repositories/{id}/branches/checkout`: Switch active branch (`{"branchName": "feature/login"}`).
- `DELETE /api/vcs/repositories/{id}/branches/{name}`: Delete branch.
- `GET /api/vcs/repositories/{id}/diff`: View pending unified diff between working tree and HEAD.

### Remote Synchronization
- `GET /api/vcs/repositories/{id}/remotes`: List configured remotes.
- `POST /api/vcs/repositories/{id}/remotes`: Add or update remote (`{"name": "origin", "url": "https://..."}`).
- `POST /api/vcs/repositories/{id}/fetch`: Fetch remote branches.
- `POST /api/vcs/repositories/{id}/pull`: Pull and merge changes.
- `POST /api/vcs/repositories/{id}/push`: Push active branch to remote.
