# ProjectSync AI — System Architecture & Design Specification

This document provides a comprehensive technical breakdown of the architecture, security model, data flow, and external integrations in **ProjectSync AI**.

---

## 1. High-Level Architecture Overview

ProjectSync AI is organized as a decoupled, modern multi-tier web application:

```mermaid
graph TD
    subgraph Browser ["Web Browser Client"]
        SPA["React 18 Single Page App (Port 5173)"]
        Storage["sessionStorage ('projectsync_auth')"]
    end

    subgraph ReverseProxy ["Vite Proxy / HTTP Gateway"]
        Proxy["Vite Dev Server (/api -> :8081)"]
    end

    subgraph BackendApp ["Spring Boot Backend (Port 8081)"]
        Filter["JwtAuthenticationFilter"]
        SecConfig["Spring SecurityFilterChain"]
        Dispatch["Spring MVC DispatcherServlet"]

        subgraph Controllers ["Controller Layer (/api/*)"]
            AuthCtrl["AuthController"]
            AgileCtrl["Project / Sprint / Task Controllers"]
            AiCtrl["FacultyAnalytics / Proposal AI"]
            VcsCtrl["VcsController"]
        end

        subgraph ServiceLayer ["Service Layer"]
            AuthSvc["AuthService & CustomUserDetailsService"]
            AgileSvc["SprintService, TaskService, WorkloadService"]
            AiSvc["GeminiApiClient & AiAssessmentService"]
            GitSvc["GitService (Eclipse JGit Engine)"]
            StorageSvc["SupabaseStorageService"]
        end

        subgraph PersistenceLayer ["Data Access Layer"]
            Repos["Spring Data JPA Repositories (19 Interfaces)"]
            MetaStore["LocalRepositoryMetadataStore"]
        end
    end

    subgraph Infrastructure ["External Infrastructure & Cloud"]
        CloudDB[("Supabase PostgreSQL DB")]
        LocalH2[("H2 Database Engine")]
        CloudStorage["Supabase Storage Service"]
        GeminiAPI["Google Gemini AI REST API"]
        DiskRepo["Local Disk Git Repositories (~/.projectsync)"]
    end

    SPA -->|Direct or Proxied HTTP| Filter
    Filter --> SecConfig
    SecConfig --> Dispatch
    Dispatch --> Controllers
    Controllers --> ServiceLayer
    ServiceLayer --> PersistenceLayer

    PersistenceLayer -.->|Default Profile| CloudDB
    PersistenceLayer -.->|Local Profile| LocalH2
    GitSvc --> DiskRepo
    AiSvc --> GeminiAPI
    StorageSvc --> CloudStorage
```

---

## 2. Frontend Layer (React 18 + Vite)

### Technology & Structure
- **Vite 5**: Provides fast Hot Module Replacement (HMR) and optimized rollup production bundles.
- **React Router DOM 6**: Handles client-side navigation, history management, and role-guarded routes via `<ProtectedRoute>`.
- **Axios HTTP Client**:
  - Configured with `baseURL: 'http://localhost:8081/api'` in `frontend/src/services/api.js`.
  - **Request Interceptor**: Automatically reads JWT from `sessionStorage` (`projectsync_auth`) and injects the `Authorization: Bearer <token>` header on every outgoing API call.
  - **Response Interceptor**: Catches `401 Unauthorized` responses and automatically clears invalid credentials to prevent stale UI state.
- **Auth Context (`AuthContext.jsx`)**:
  - Exposes `user`, `token`, `login()`, `logout()`, `isAuthenticated`, and helper methods across the React component hierarchy.

### Route Protection Model
The frontend uses route wrappers to restrict access by role:
- **Public**: `/login`, `/register`.
- **Student Only**: `/student/dashboard`, `/student/profile`, `/student/team`, `/student/proposal`, `/student/projects`.
- **Faculty Only**: `/faculty/dashboard`, `/faculty/profile`, `/faculty/proposals`, `/faculty/projects`.
- **Shared / Developer**: `/student/vcs` and `/vcs` (accessible by both roles, with development sandbox fallback).

---

## 3. Backend Layer (Spring Boot 4.1.1)

### Component Layers
1. **Controller Layer**:
   - 19 `@RestController` classes exposed under the `/api` prefix.
   - Responsible for request validation, path parsing, user principal extraction, and standard HTTP response codes (`200 OK`, `201 Created`, `400 Bad Request`, `403 Forbidden`, `404 Not Found`).
2. **Service Layer**:
   - Contains business logic, cross-team authorization rules, team membership validation, and audit logging.
3. **Repository Layer**:
   - 19 interfaces extending `JpaRepository<T, ID>`, providing declarative CRUD and custom JPQL queries.
4. **Exception Handling**:
   - Handled centrally by `GlobalExceptionHandler.java`, ensuring all validation and runtime errors return standardized JSON error objects:
     ```json
     {
       "timestamp": "2026-10-20T16:00:00Z",
       "status": 400,
       "error": "Bad Request",
       "message": "Sprint start date cannot be after end date"
     }
     ```

---

## 4. Security & Authorization Model

### Authentication Flow
```mermaid
sequenceDiagram
    autonumber
    actor User as Student / Faculty
    participant Browser as React Frontend
    participant Filter as JwtAuthenticationFilter
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthService
    participant DB as PostgreSQL / H2

    User->>Browser: Enters email and password
    Browser->>AuthCtrl: POST /api/auth/login {email, password}
    AuthCtrl->>AuthSvc: authenticate(email, password)
    AuthSvc->>DB: findByEmail(email)
    DB-->>AuthSvc: User Entity + BCrypt Hash
    AuthSvc->>AuthSvc: PasswordEncoder.matches(password, hash)
    AuthSvc->>AuthSvc: Generate signed JWT (HMAC-SHA256)
    AuthSvc-->>AuthCtrl: AuthResponse {token, role, email, fullName}
    AuthCtrl-->>Browser: 200 OK + JWT JSON
    Browser->>Browser: Save to sessionStorage ('projectsync_auth')
    Browser-->>User: Redirect to Role Dashboard
```

### Request Authorization Lifecycle
For every subsequent request:
1. `JwtAuthenticationFilter` intercepts the request.
2. Extracts `Authorization: Bearer <token>` header.
3. Validates signature and expiration against `jwt.secret`.
4. Extracts email and role claim (`STUDENT` or `FACULTY`).
5. Loads user principal and stores `UsernamePasswordAuthenticationToken` in Spring's `SecurityContextHolder`.
6. Service-level checks verify fine-grained permissions:
   - Is this student a member of the requested project's team?
   - Is the user the leader of this team?
   - Is this task assigned to this student?
   - Does this comment belong to the current user?

---

## 5. Embedded Git Engine (Eclipse JGit Integration)

ProjectSync AI features a native Java Git version control engine that does not require an external `git` binary installed on the server:

- **Library**: Eclipse JGit (`org.eclipse.jgit 7.1.0`).
- **Storage Location**: Configurable via `projectsync.vcs.storage-path` (defaults to `~/.projectsync/repositories`).
- **Metadata Management**: `LocalRepositoryMetadataStore.java` records repository metadata, owners, branch information, and initialization state in JSON format on disk.
- **Core Operations**:
  - `init`: Creates new bare or working Git repositories on disk.
  - `clone`: Clones local or remote repositories into isolated project workspaces.
  - `stage` / `unstage`: Manages the JGit Git Index (`dircache`).
  - `commit`: Creates cryptographically signed commits with commit author metadata.
  - `diff`: Uses JGit `DiffFormatter` and `RawTextComparator` to compute unified diffs for code reviews.
  - `sync`: Manages remote refspecs (`fetch`, `pull`, `push`) with configurable remote URLs.
- **Concurrency & Safety**:
  - JGit memory-mapped file cache configured with `WindowCacheConfig.setPackedGitMMAP(false)` to prevent file locking issues on Windows OS.

---

## 6. Gemini AI Architecture

The platform uses Google's Gemini generative AI model to automate administrative and academic workflows:

```mermaid
sequenceDiagram
    actor Faculty as Faculty Evaluator
    participant Ctrl as FacultyAnalyticsController
    participant Svc as AiAssessmentService
    participant Client as GeminiApiClient
    participant Google as Gemini Cloud API

    Faculty->>Ctrl: GET /api/faculty/analytics/projects/{id}/code-inspection
    Ctrl->>Svc: inspectProjectCode(projectId)
    Svc->>Svc: Aggregate student commits, tasks, and file diffs
    Svc->>Client: generateJson(structuredInspectionPrompt)
    Client->>Google: HTTPS POST /v1beta/models/gemini-flash-latest:generateContent
    Google-->>Client: Raw JSON Candidates
    Client->>Client: Parse and validate JSON response
    Client-->>Svc: AiCodeQualityInspectionResponse DTO
    Svc-->>Ctrl: Returns structured quality metrics
    Ctrl-->>Faculty: Render Code Inspection Dashboard
```

- **Resilience & Fallback**: If the primary model endpoint (`gemini-flash-latest`) encounters rate limits or errors, `GeminiApiClient` automatically fails over to `gemini-3.5-flash`.
- **Graceful Degradation**: If no `GEMINI_API_KEY` is configured, an informative exception (`GEMINI_API_KEY is not configured`) is handled cleanly without crashing the Spring application context.

---

## 7. Supabase Storage Integration

- **Purpose**: Stores binary project deliverables (ZIP files, PDF design documents, diagrams).
- **Service**: `SupabaseStorageService.java` using Spring's HTTP `RestClient`.
- **Security**: Uploads and downloads are authenticated using the Supabase Service Role Key (`SUPABASE_SERVICE_ROLE_KEY`).
- **Access Control**: Downloads verify project team membership before issuing access.
