package com.projectsync.backend.service;

import com.projectsync.backend.dto.AiSprintBreakdownResponse;
import com.projectsync.backend.dto.AiSprintBreakdownResponse.SprintBreakdown;
import com.projectsync.backend.dto.AiSprintBreakdownResponse.UserStory;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.ProjectProposal;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiSprintBreakdownService {

    public AiSprintBreakdownResponse generateBreakdown(ProjectProposal project) {
        String title = project.getTitle();
        String tech = project.getTechnologyStack() != null ? project.getTechnologyStack() : "Fullstack";

        List<UserStory> sprint1Stories = List.of(
                new UserStory("US-101", "Database Schema & Entity Definitions",
                        "As a backend developer, I want to design and migrate PostgreSQL relational schemas with foreign keys so that system state is normalized.",
                        List.of("ER diagrams validated", "JPA entity models annotated", "Liquibase/Hibernate migrations executed successfully"),
                        5, "Database / Backend Engineer"),
                new UserStory("US-102", "User Authentication & JWT Tokens",
                        "As a user, I want to securely log in with email/password and obtain a signed JWT token so that my session is authenticated.",
                        List.of("BCrypt password hashing implemented", "JWT filter extracts Bearer token", "Invalid logins return HTTP 401"),
                        5, "Backend Lead"),
                new UserStory("US-103", "UI Design System & Shell Layout",
                        "As a user, I want a responsive dashboard shell with navigation links and theme tokens so that navigation is frictionless.",
                        List.of("Sidebar renders student and faculty routes", "Mobile hamburger drawer operates properly", "CSS variable tokens defined"),
                        3, "Frontend Lead")
        );

        List<UserStory> sprint2Stories = List.of(
                new UserStory("US-201", "Domain Service Logic & Core Business APIs",
                        "As a system user, I want core CRUD operations for " + title + " exposed via secure REST endpoints so that workflows can execute.",
                        List.of("Service layer handles transaction boundaries", "DTO validation rules in place (@NotNull, @Size)", "HTTP 200/201 responses returned"),
                        8, "Backend Lead"),
                new UserStory("US-202", "Interactive Client Views & Form Validation",
                        "As an end user, I want interactive form inputs with real-time feedback and error messages so that invalid data is caught before submission.",
                        List.of("Axios service interceptors handle token injection", "Form fields display validation errors", "Loading spinners display during async calls"),
                        5, "Frontend Lead"),
                new UserStory("US-203", "Role-Based Access Control Rules",
                        "As an administrator, I want endpoints restricted by user role (STUDENT vs FACULTY) so that unauthorized access is blocked.",
                        List.of("Spring Security method security active", "Unauthorized requests return HTTP 403 Forbidden", "UI conditionally hides restricted action buttons"),
                        3, "Security / Backend Engineer")
        );

        List<UserStory> sprint3Stories = List.of(
                new UserStory("US-301", "Real-Time Analytics & Progress Metrics",
                        "As a supervisor, I want to view operational charts and progress statistics so that team trajectory is measurable.",
                        List.of("Aggregate SQL queries compute metrics", "Chart visualizer renders responsive bars", "Empty states handled cleanly"),
                        5, "AI/ML / Data Engineer"),
                new UserStory("US-302", "Notification & Activity Feed Engine",
                        "As a team member, I want an activity feed showing recent milestones and status updates so that team actions are transparent.",
                        List.of("Events logged to audit log table", "UI queries timeline feed", "Timestamp formatting follows local conventions"),
                        3, "Fullstack Developer")
        );

        List<UserStory> sprint4Stories = List.of(
                new UserStory("US-401", "Security Hardening & Penetration Verification",
                        "As a QA engineer, I want automated security vulnerability audits and penetration tests so that no OWASP top 10 flaws remain.",
                        List.of("SQL injection tests pass", "CORS origins locked to authorized hosts", "Rate-limiting checks verified"),
                        3, "DevOps / QA Engineer"),
                new UserStory("US-402", "Containerized Deployment & Final Documentation",
                        "As a deployer, I want Docker compose definitions and OpenAPI documentation so that the platform can be deployed reliably.",
                        List.of("Dockerfile compiles minimal production image", "Docker compose stands up backend and database", "Comprehensive README with setup instructions"),
                        5, "DevOps Lead")
        );

        List<SprintBreakdown> sprints = List.of(
                new SprintBreakdown(1, "Sprint 1: Architecture & Foundations",
                        "Establish the relational data tier, authentication infrastructure, and base responsive design system.",
                        13, sprint1Stories),
                new SprintBreakdown(2, "Sprint 2: Core Domain Services & Workflows",
                        "Deliver functional domain APIs, interactive user forms, and role-based security boundaries.",
                        16, sprint2Stories),
                new SprintBreakdown(3, "Sprint 3: Analytics, Insights & Integration",
                        "Synthesize analytics metrics, activity feeds, and cross-tier integration testing.",
                        8, sprint3Stories),
                new SprintBreakdown(4, "Sprint 4: Quality Assurance, Hardening & Delivery",
                        "Execute security audits, performance profiling, containerization, and final release documentation.",
                        8, sprint4Stories)
        );

        int total = sprints.stream().mapToInt(SprintBreakdown::plannedPoints).sum();

        return new AiSprintBreakdownResponse(
                project.getId(),
                project.getTitle(),
                total,
                sprints
        );
    }
}
