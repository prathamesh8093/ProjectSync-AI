# ProjectSync AI

> An Intelligent Collaborative Software Engineering Platform for Real-Time Project Tracking and Fair Contribution Assessment

## Project Overview

ProjectSync AI is an academic software-project collaboration platform designed to connect project proposal management, Agile project execution, student collaboration, progress tracking, workload visibility, and faculty monitoring in one system.

The project is being developed incrementally by the team. The current repository reflects the work completed through **Member 2**.

## Current Project Progress

| Stage | Status | Scope |
|---|---|---|
| Member 1 | ✅ Completed | Authentication, profiles, teams, project proposals, faculty review, role/authorization foundations |
| Member 2 | ✅ Completed | Agile project workspace, sprints, tasks, assignment, Kanban, collaboration, progress, workload, faculty monitoring, project files |
| Member 3 | ⏳ Pending / Next Stage | Remaining planned integrations, enhancements, testing, documentation and final project work |

### Current Milestone

**Member 2 Completion — Agile Project Management & Collaborative Work**

The current implementation provides the core Agile execution and monitoring layer after a project proposal has been approved.

## Implemented Features

### Authentication & Authorization
- JWT-based authentication
- Student and faculty role restrictions
- Team membership validation
- Approved-project access control
- Cross-team access protection
- Leader-only project-management operations where required
- Ownership checks for student-owned collaborative resources

### Student & Team Workflow
- Student profiles
- Faculty profiles
- Team management
- Project proposal submission
- Faculty proposal review
- Approved proposal workflow

### Agile Project Workspace
- Project workspace for approved projects
- Project status and project information
- Team/project access validation
- Project progress view

### Sprint Management
- Create and manage sprints
- Sprint goals
- Start/end date validation
- Sprint statuses: `PLANNED`, `ACTIVE`, `COMPLETED`
- Active-sprint validation
- Leader-based sprint management

### Task Management
- Task creation
- Task assignment
- Task descriptions
- Task priorities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- Task statuses: `TODO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`
- Task progress tracking
- Student task access restrictions

### Kanban Workflow

Tasks are organized into four workflow columns:

```text
TODO → IN PROGRESS → IN REVIEW → DONE
```

The Kanban board provides a visual representation of project execution.

### Task Collaboration
- Task comments
- Comment update/delete ownership
- Task history
- Audit information for task changes

### Project Documents
- Create project documents
- View project documents
- Update project documents
- Delete project documents
- Author and updater information
- Approved-project/team access validation

### Project Progress
- Overall project progress
- Total task count
- Completed tasks
- In-progress tasks
- In-review tasks
- To-do tasks

Task status is mapped to progress consistently:

```text
TODO        → 0%
IN_PROGRESS → 50%
IN_REVIEW   → 75%
DONE        → 100%
```

### Workload Monitoring
- Team-member workload visibility
- Task distribution across members
- Members with zero assigned tasks are included
- Provides the foundation for future intelligent task recommendation

### Faculty Project Monitoring
Faculty can monitor approved projects including:
- Projects
- Teams
- Sprints
- Tasks
- Assignees
- Task status
- Progress
- Workload

Faculty monitoring is designed primarily for project visibility and monitoring rather than automatic modification of student Agile work.

### Project File Storage
- Project file metadata
- Private Supabase Storage integration
- Upload/download/delete application flow
- Project/team/approval authorization checks

> Note: The application-side project-file storage integration is implemented. The development environment has previously experienced Supabase hostname/routing issues during Storage upload testing; this remains an environment/infrastructure item to verify separately.

## Technology Stack

### Frontend
- React.js
- Vite
- React Router
- Axios
- CSS

### Backend
- Java 21
- Spring Boot 4.1.1
- Spring Security
- JWT authentication
- REST APIs
- JPA / Hibernate
- Maven

### Database & Storage
- PostgreSQL
- Supabase
- Supabase Storage

### Development & Version Control
- npm
- Maven Wrapper
- Git
- GitHub

## High-Level Architecture

```text
Student / Faculty
       |
       v
React Frontend
       |
       | REST API / Axios
       v
Spring Boot Backend
       |
       +----------------------+
       |                      |
       v                      v
PostgreSQL / Supabase    Supabase Storage
       |
       v
Project / Team / Sprint / Task /
Comments / History / Documents /
Progress / Workload Data
```

## Agile Workflow

```text
Project Proposal
       |
       v
Faculty Review
       |
       v
APPROVED Proposal
       |
       v
Agile Project Workspace
       |
       +----> Sprint
       |        |
       |        +----> Tasks
       |                 |
       |                 +----> Assignment
       |                 +----> Kanban
       |                 +----> Progress
       |                 +----> Comments
       |                 +----> History
       |
       +----> Project Documents
       |
       +----> Workload
       |
       v
Faculty Project Monitoring
```

## Security Model

Protected Agile resources use JWT authentication and service-layer authorization.

Authorization checks include:
- Authenticated user
- User role
- Student/faculty permissions
- Team membership
- Project membership
- Approved proposal status
- Project leader permissions
- Assigned-task ownership
- Comment ownership
- Cross-team access protection

The system uses backend authorization rather than relying only on frontend visibility.

## Repository Structure

```text
ProjectSync-AI/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── mvnw.cmd
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── .gitignore
└── README.md
```

## Local Development

### Backend

Required environment variables:

```text
SUPABASE_DB_URL
JWT_SECRET
SUPABASE_SERVICE_ROLE_KEY
```

From the backend directory:

```cmd
cd /d D:\Research\ProjectSync-AI\backend
mvnw.cmd spring-boot:run
```

Backend runs on:

```text
http://localhost:8081
```

### Frontend

```cmd
cd /d D:\Research\ProjectSync-AI\frontend
npm install
npm run dev
```

## Verification

The Member 2 implementation has been verified through:
- Backend compilation
- Frontend production build
- Protected API authentication checks
- Student Agile workspace testing
- Sprint/task workflow testing
- Kanban UI verification
- Task comments and history testing
- Project document testing
- Project progress verification
- Workload verification
- Faculty project monitoring verification
- Authorization/security checks

## Current Research Boundary

The following are **research objectives / future enhancements**, not claimed as fully implemented in Member 2:

- AI-based skill and interest assessment
- Intelligent AI task recommendation
- GitHub commit / pull-request integration
- AI-based contribution scoring
- Advanced contribution analytics
- Gamification
- Advanced learning analytics
- Automated notification integrations

The current Member 2 implementation provides the structured Agile/project-activity data foundation required for these future capabilities.

## Member 2 Milestone

**Status: COMPLETED ✅**

Member 2 delivered the Agile project-management and collaborative-work layer:

```text
Approved Proposal
      ↓
Agile Project
      ↓
Sprints
      ↓
Tasks & Assignment
      ↓
Kanban
      ↓
Comments + History
      ↓
Documents
      ↓
Progress + Workload
      ↓
Faculty Monitoring
```

The Member 2 branch is:

```text
member2-agile
```

The completed work has been pushed to GitHub and submitted for integration into `main`.

## Next Development Stage

The next stage should build on the completed Member 1 + Member 2 foundation without duplicating existing authentication, team, proposal, Agile, or monitoring functionality.

Future work may include the remaining planned AI, GitHub, analytics, notification, gamification, and final project requirements.

## Project Repository

https://github.com/prathamesh8093/ProjectSync-AI
