package com.projectsync.backend.service;

import com.projectsync.backend.dto.WorkloadResponse;
import com.projectsync.backend.entity.Project;
import com.projectsync.backend.entity.Task;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.ProjectRepository;
import com.projectsync.backend.repository.TaskRepository;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkloadService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public WorkloadService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {

        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkloadResponse> getMyProjectWorkload(String email) {

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        TeamMember currentMembership = teamMemberRepository
                .findByStudentUser(currentUser)
                .orElseThrow(() -> new RuntimeException("You are not a member of a team."));

        Project project = projectRepository
                .findByTeam(currentMembership.getTeam())
                .orElseThrow(() -> new RuntimeException("Project not found."));

        if (project.getProposal() == null
                || !"APPROVED".equalsIgnoreCase(project.getProposal().getStatus())) {
            throw new RuntimeException(
                    "Agile workload is available only for an approved project."
            );
        }

        List<TeamMember> teamMembers =
                teamMemberRepository.findByTeam(currentMembership.getTeam());

        List<Task> projectTasks =
                taskRepository.findByProjectOrderByCreatedAtAsc(project);

        List<WorkloadResponse> workload = new ArrayList<>();

        for (TeamMember teamMember : teamMembers) {

            User member = teamMember.getStudentUser();

            int totalTasks = 0;
            int todoTasks = 0;
            int inProgressTasks = 0;
            int inReviewTasks = 0;
            int completedTasks = 0;
            int totalProgress = 0;

            for (Task task : projectTasks) {

                if (task.getAssignedTo() == null
                        || !task.getAssignedTo().getId().equals(member.getId())) {
                    continue;
                }

                totalTasks++;

                String status = task.getStatus();

                if ("TODO".equalsIgnoreCase(status)) {
                    todoTasks++;
                } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                    inProgressTasks++;
                } else if ("IN_REVIEW".equalsIgnoreCase(status)) {
                    inReviewTasks++;
                } else if ("DONE".equalsIgnoreCase(status)) {
                    completedTasks++;
                }

                if (task.getProgress() != null) {
                    totalProgress += task.getProgress();
                }
            }

            int averageProgress = totalTasks == 0
                    ? 0
                    : Math.round((float) totalProgress / totalTasks);

            workload.add(
                    new WorkloadResponse(
                            member.getId(),
                            member.getFullName(),
                            totalTasks,
                            todoTasks,
                            inProgressTasks,
                            inReviewTasks,
                            completedTasks,
                            averageProgress
                    )
            );
        }

        return workload;
    }
}