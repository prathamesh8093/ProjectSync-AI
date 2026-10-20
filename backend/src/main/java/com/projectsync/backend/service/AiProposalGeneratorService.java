package com.projectsync.backend.service;

import com.projectsync.backend.dto.AiProposalDraftRequest;
import com.projectsync.backend.dto.AiProposalDraftResponse;
import com.projectsync.backend.dto.AiProposalDraftResponse.ModuleTimeline;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AiProposalGeneratorService {

    private final GeminiApiClient gemini;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public AiProposalGeneratorService(GeminiApiClient gemini, ObjectMapper objectMapper) {
        this.gemini = gemini;
        this.objectMapper = objectMapper;
    }

    public AiProposalDraftResponse generateProposal(AiProposalDraftRequest request) {
        String idea = request.idea().trim();
        String tech = request.technologyStack() != null && !request.technologyStack().isBlank()
                ? request.technologyStack().trim()
                : "React, Spring Boot, PostgreSQL, Docker";
        int weeks = request.durationWeeks() != null && request.durationWeeks() > 0 ? request.durationWeeks() : 12;

        // Try AI generation via Gemini
        try {
            return generateWithGemini(idea, tech, weeks);
        } catch (Exception e) {
            System.out.println("Gemini proposal drafting unavailable; using intelligent academic generator: " + e.getMessage());
            return generateHeuristic(idea, tech, weeks);
        }
    }

    private AiProposalDraftResponse generateWithGemini(String idea, String tech, int weeks) throws Exception {
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusWeeks(weeks);

        String prompt = """
                You are a senior academic software engineering advisor.
                Draft a high-quality, professional project proposal based on the following input:
                Project Idea / Domain: %s
                Technology Stack: %s
                Duration: %d weeks (Starting %s, Ending %s)

                Requirements:
                Generate a structured JSON object with the following fields:
                - title: Clear, professional academic title (under 120 chars)
                - abstractText: 2-3 paragraph comprehensive academic abstract
                - objectives: 4-6 specific, measurable technical objectives (numbered list format)
                - literatureReview: Summary of current state-of-the-art and existing frameworks in this domain
                - researchGaps: Critical limitations, bottlenecks, or security/scalability gaps in existing systems that this project solves
                - technologyStack: Categorized list of technologies with architectural rationale
                - futureScope: 3-5 future expansion possibilities and scalability prospects
                - modules: An array of 4 distinct sequential modules dividing the project timeline evenly from %s to %s.
                  Each module must have:
                  "moduleNumber": "Module 1", "moduleName": "...", "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD", "deliverables": "..."

                Respond ONLY with a JSON object.
                """.formatted(idea, tech, weeks, start, end, start, end);

        String json = gemini.generateJson(prompt);
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});

        String title = String.valueOf(map.getOrDefault("title", "Project Proposal"));
        String abstractText = String.valueOf(map.getOrDefault("abstractText", ""));
        String objectives = String.valueOf(map.getOrDefault("objectives", ""));
        String literatureReview = String.valueOf(map.getOrDefault("literatureReview", ""));
        String researchGaps = String.valueOf(map.getOrDefault("researchGaps", ""));
        String technologyStack = String.valueOf(map.getOrDefault("technologyStack", tech));
        String futureScope = String.valueOf(map.getOrDefault("futureScope", ""));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawModules = (List<Map<String, Object>>) map.get("modules");
        List<ModuleTimeline> modules = new ArrayList<>();
        if (rawModules != null) {
            for (Map<String, Object> m : rawModules) {
                modules.add(new ModuleTimeline(
                        String.valueOf(m.getOrDefault("moduleNumber", "Module")),
                        String.valueOf(m.getOrDefault("moduleName", "")),
                        String.valueOf(m.getOrDefault("startDate", "")),
                        String.valueOf(m.getOrDefault("endDate", "")),
                        String.valueOf(m.getOrDefault("deliverables", ""))
                ));
            }
        }

        if (modules.isEmpty()) {
            modules = buildDefaultTimeline(start, weeks);
        }

        String formattedDescription = assembleDescription(abstractText, literatureReview, researchGaps, futureScope, modules);
        return new AiProposalDraftResponse(
                title,
                abstractText,
                objectives,
                literatureReview,
                researchGaps,
                technologyStack,
                futureScope,
                modules,
                formattedDescription,
                objectives
        );
    }

    private AiProposalDraftResponse generateHeuristic(String idea, String tech, int weeks) {
        LocalDate start = LocalDate.now().plusDays(2);
        List<ModuleTimeline> modules = buildDefaultTimeline(start, weeks);

        String capitalizedTitle = Character.toUpperCase(idea.charAt(0)) + idea.substring(1);
        if (!capitalizedTitle.toLowerCase().contains("system") && !capitalizedTitle.toLowerCase().contains("platform")) {
            capitalizedTitle += " Management & Analytics Platform";
        }

        String abstractText = """
                In modern software ecosystems, addressing challenges in %s requires reliable, modular, and data-driven computational architectures. Traditional manual workflows and legacy monolithic implementations suffer from poor responsiveness, data fragmentation, and high administrative overhead. This project presents %s, an engineered end-to-end software solution designed to automate core operations, enhance collaboration, and deliver real-time operational insights.
                
                The proposed application leverages a decoupled client-server architecture with %s. The primary goal is to provide a seamless user interface coupled with secure, resilient backend micro-services, ensuring high availability and fault-tolerant transaction processing. By unifying data pipelines and role-based access control, the system significantly mitigates operational latency and empowers stakeholders with transparent oversight.
                """.formatted(idea, capitalizedTitle, tech);

        String objectives = """
                1. Architect a scalable, responsive user application with %s to deliver intuitive user workflows.
                2. Implement high-throughput, secure RESTful APIs with stateless authentication and strict role-based access control.
                3. Design an optimized relational data model to ensure ACID compliance, referential integrity, and efficient query performance.
                4. Establish automated continuous integration pipelines with rigorous unit, integration, and security testing.
                5. Provide real-time data synchronization and audit logging for comprehensive activity tracking.
                """.formatted(tech);

        String literatureReview = """
                Contemporary research into %s highlights a paradigm shift from paper-based and monolithic systems toward service-oriented cloud architectures. Studies demonstrate that modern web platforms utilizing reactive component architectures and asynchronous API communication achieve superior throughput and sub-second response times compared to legacy systems.
                
                However, prevailing academic literature also indicates that existing implementations often compromise on modular security boundaries, robust exception handling, and granular audit trails, making them vulnerable to concurrency bottlenecks and unauthorized privilege escalation.
                """.formatted(idea);

        String researchGaps = """
                1. Lack of Automated Workflow Coordination: Most existing solutions require manual intervention for status handoffs, leading to prolonged cycle times.
                2. Inadequate Real-Time Analytics & Tracking: Stakeholders lack live visibility into operational metrics and progress bottlenecks.
                3. Fragmented Security & Data Isolation: Role permissions are frequently hardcoded or loosely enforced, exposing critical entities to data leakage.
                4. Poor Scalability under Peak Loads: Traditional relational queries without indexed caching suffer from severe performance degradation during high concurrent traffic.
                """;

        String futureScope = """
                1. Cloud-Native Elastic Auto-Scaling: Deploying containerized services on Kubernetes with automated horizontal pod autoscaling.
                2. Machine Learning Predictive Analytics: Integrating predictive models to forecast operational trends, anomalies, and resource allocation.
                3. Native Mobile Application: Extending access via React Native or Flutter mobile apps with push notification alerts.
                4. Decentralized Audit Ledger: Incorporating cryptographic event logging for immutable compliance verification.
                """;

        String technologyStack = """
                - Frontend Layer: React 18 with modern component architecture, Vite bundler, Axios HTTP client, responsive CSS.
                - Backend Layer: Java 21 & Spring Boot (Spring Security, Spring Data JPA, WebMVC, Bean Validation).
                - Database & Storage: PostgreSQL relational database with foreign key constraints, connection pooling (HikariCP), and transactional integrity.
                - Security & Identity: Stateless JWT token authentication with BCrypt password hashing and CORS origin isolation.
                - DevOps & Tooling: Docker containerization, Maven build lifecycle, Git version control.
                """;

        String formattedDescription = assembleDescription(abstractText, literatureReview, researchGaps, futureScope, modules);

        return new AiProposalDraftResponse(
                capitalizedTitle,
                abstractText,
                objectives,
                literatureReview,
                researchGaps,
                technologyStack,
                futureScope,
                modules,
                formattedDescription,
                objectives
        );
    }

    private List<ModuleTimeline> buildDefaultTimeline(LocalDate start, int totalWeeks) {
        int m1Weeks = Math.max(2, totalWeeks / 4);
        int m2Weeks = Math.max(3, (totalWeeks - m1Weeks) / 3);
        int m3Weeks = Math.max(3, (totalWeeks - m1Weeks - m2Weeks) / 2);
        int m4Weeks = Math.max(2, totalWeeks - m1Weeks - m2Weeks - m3Weeks);

        LocalDate m1Start = start;
        LocalDate m1End = m1Start.plusWeeks(m1Weeks).minusDays(1);

        LocalDate m2Start = m1End.plusDays(1);
        LocalDate m2End = m2Start.plusWeeks(m2Weeks).minusDays(1);

        LocalDate m3Start = m2End.plusDays(1);
        LocalDate m3End = m3Start.plusWeeks(m3Weeks).minusDays(1);

        LocalDate m4Start = m3End.plusDays(1);
        LocalDate m4End = m4Start.plusWeeks(m4Weeks).minusDays(1);

        return List.of(
                new ModuleTimeline("Module 1", "System Requirements & Architectural Specification",
                        m1Start.format(DATE_FMT), m1End.format(DATE_FMT),
                        "Software Requirements Specification (SRS), ER Diagrams, UI/UX Wireframes, and API Contract Definitions."),
                new ModuleTimeline("Module 2", "Core Backend Services & Database Engine",
                        m2Start.format(DATE_FMT), m2End.format(DATE_FMT),
                        "PostgreSQL Schema Migrations, JPA Entities, Spring Boot Service layer, Security/JWT Authentication filters."),
                new ModuleTimeline("Module 3", "Interactive Frontend & Fullstack Integration",
                        m3Start.format(DATE_FMT), m3End.format(DATE_FMT),
                        "React Component Suite, State Management, API Service Interceptors, Form Validations, and Dashboard Views."),
                new ModuleTimeline("Module 4", "Quality Assurance, Security Auditing & Deployment",
                        m4Start.format(DATE_FMT), m4End.format(DATE_FMT),
                        "End-to-End Testing, Security Vulnerability Scanning, Docker Containerization, CI/CD Pipeline, and Final Project Documentation.")
        );
    }

    private String assembleDescription(String abs, String lit, String gaps, String future, List<ModuleTimeline> modules) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 1. Abstract\n").append(abs.trim()).append("\n\n");
        sb.append("### 2. Literature Review\n").append(lit.trim()).append("\n\n");
        sb.append("### 3. Existing System Gaps & Problem Statement\n").append(gaps.trim()).append("\n\n");
        sb.append("### 4. Future Scope & Scalability\n").append(future.trim()).append("\n\n");
        sb.append("### 5. Timelined Module Breakdown & Deliverables\n");
        sb.append("| Module | Start Date | End Date | Deliverables |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        for (ModuleTimeline m : modules) {
            sb.append("| **").append(m.moduleNumber()).append(": ").append(m.moduleName()).append("** | ")
                    .append(m.startDate()).append(" | ")
                    .append(m.endDate()).append(" | ")
                    .append(m.deliverables()).append(" |\n");
        }
        return sb.toString();
    }
}
