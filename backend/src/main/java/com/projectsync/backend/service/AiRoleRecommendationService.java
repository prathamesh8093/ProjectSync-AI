package com.projectsync.backend.service;

import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.StudentAssessment;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.StudentAssessmentRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRoleRecommendationService {

    public record RoleRecommendation(UUID studentUserId, String fullName, String role, String reason) {}

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final StudentAssessmentRepository assessmentRepository;
    private final GeminiApiClient gemini;
    private final ObjectMapper objectMapper;

    public AiRoleRecommendationService(TeamRepository teamRepository,
                                       TeamMemberRepository teamMemberRepository,
                                       ProjectRepository projectRepository,
                                       StudentAssessmentRepository assessmentRepository,
                                       GeminiApiClient gemini,
                                       ObjectMapper objectMapper) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.projectRepository = projectRepository;
        this.assessmentRepository = assessmentRepository;
        this.gemini = gemini;
        this.objectMapper = objectMapper;
    }

    public List<RoleRecommendation> recommendRoles(User currentUser, UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        teamMemberRepository.findByTeamAndStudentUser(team, currentUser)
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team."));

        Project proposal = projectRepository.findByTeam(team)
                .orElseThrow(() -> new BadRequestException("Create a project proposal before requesting role recommendations."));

        List<User> students = teamMemberRepository.findByTeam(team).stream()
                .map(TeamMember::getStudentUser)
                .toList();

        // Try AI generation via Gemini first
        try {
            String profiles = students.stream()
                    .map(s -> "- id=" + s.getId() + ", name=" + s.getFullName() + ", "
                            + assessmentRepository.findById(s.getId()).map(this::describe).orElse("no self-assessment submitted"))
                    .collect(Collectors.joining("\n"));

            String prompt = """
                    You are assigning roles in a university software project team.
                    Project title: %s
                    Description: %s
                    Tech stack: %s

                    Team members (self-assessed skills, 1-10):
                    %s

                    Recommend one distinct role per member (e.g. Frontend Lead, Backend Architect,
                    Database & DevOps, QA & Documentation Lead, AI/ML Engineer) and explain why in one or two sentences.
                    Respond ONLY with a JSON array: [{"studentUserId": "<id>", "role": "<role>", "reason": "<reason>"}]
                    """.formatted(proposal.getTitle(), proposal.getDescription(),
                    Objects.requireNonNullElse(proposal.getTechnologyStack(), "not specified"), profiles);

            List<Map<String, String>> raw = objectMapper.readValue(gemini.generateJson(prompt), new TypeReference<>() {});

            Map<String, User> byId = students.stream().collect(Collectors.toMap(s -> s.getId().toString(), s -> s));
            List<RoleRecommendation> result = raw.stream()
                    .filter(r -> byId.containsKey(r.get("studentUserId")))
                    .map(r -> {
                        User s = byId.get(r.get("studentUserId"));
                        return new RoleRecommendation(s.getId(), s.getFullName(), r.get("role"), r.get("reason"));
                    })
                    .toList();

            if (!result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            // Log fallback and proceed with smart heuristic role assignment
            System.out.println("Gemini AI role recommendation unavailable; falling back to heuristic assignment: " + e.getMessage());
        }

        return recommendRolesHeuristic(proposal, students);
    }

    private List<RoleRecommendation> recommendRolesHeuristic(Project proposal, List<User> students) {
        Map<UUID, StudentAssessment> assessments = new HashMap<>();
        for (User s : students) {
            assessmentRepository.findById(s.getId()).ifPresent(a -> assessments.put(s.getId(), a));
        }

        List<RoleRecommendation> recommendations = new ArrayList<>();
        Set<String> assignedRoles = new HashSet<>();

        for (int i = 0; i < students.size(); i++) {
            User s = students.get(i);
            StudentAssessment a = assessments.get(s.getId());
            String role;
            String reason;

            if (a != null) {
                int fe = a.getFrontendScore();
                int be = a.getBackendScore();
                int db = a.getDatabaseScore();
                int devops = a.getDevopsScore();
                int ml = a.getMlAiScore();
                String interest = a.getPrimaryInterest() != null ? a.getPrimaryInterest().toUpperCase() : "";

                if ((ml >= 7 || interest.contains("AI") || interest.contains("ML")) && !assignedRoles.contains("AI/ML Engineer")) {
                    role = "AI/ML Engineer";
                    reason = "Strong aptitude in machine learning models and intelligent data systems (Skill Score: " + ml + "/10).";
                } else if (fe >= be && fe >= db && !assignedRoles.contains("Frontend Lead")) {
                    role = "Frontend Lead";
                    reason = "High proficiency in user interface development and component design (Skill Score: " + fe + "/10).";
                } else if (be >= db && !assignedRoles.contains("Backend Architect")) {
                    role = "Backend Architect";
                    reason = "Excels in server-side logic, API endpoints, and system business services (Skill Score: " + be + "/10).";
                } else if (db >= devops && !assignedRoles.contains("Database & Cloud Engineer")) {
                    role = "Database & Cloud Engineer";
                    reason = "Specialized in relational schema design, query optimization, and storage (Skill Score: " + db + "/10).";
                } else if (!assignedRoles.contains("DevOps & QA Lead")) {
                    role = "DevOps & QA Lead";
                    reason = "Suited for CI/CD pipeline automation, Docker builds, and testing (Skill Score: " + devops + "/10).";
                } else {
                    role = "Fullstack Developer";
                    reason = "Well-balanced skill coverage supporting both client and server development.";
                }
            } else {
                if (i == 0 && !assignedRoles.contains("Team Leader & Backend Architect")) {
                    role = "Team Leader & Backend Architect";
                    reason = "Assigned core architectural leadership for project milestones and services.";
                } else if (!assignedRoles.contains("Frontend Lead")) {
                    role = "Frontend Lead";
                    reason = "Assigned primary user interface and client-side design.";
                } else if (!assignedRoles.contains("Database & DevOps Engineer")) {
                    role = "Database & DevOps Engineer";
                    reason = "Responsible for data persistence and infrastructure deployment.";
                } else {
                    role = "Software Engineer";
                    reason = "Contributes across fullstack implementation and code deliverables.";
                }
            }

            assignedRoles.add(role);
            recommendations.add(new RoleRecommendation(s.getId(), s.getFullName(), role, reason));
        }

        return recommendations;
    }

    private String describe(StudentAssessment a) {
        return "Frontend=%d, Backend=%d, Database=%d, DevOps=%d, ML/AI=%d, interest=%s, past projects=%s".formatted(
                a.getFrontendScore(), a.getBackendScore(), a.getDatabaseScore(), a.getDevopsScore(), a.getMlAiScore(),
                a.getPrimaryInterest(), a.getPastProjects());
    }
}
