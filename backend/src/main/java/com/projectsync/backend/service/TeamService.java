package com.projectsync.backend.service;

import com.projectsync.backend.dto.AddTeamMemberRequest;
import com.projectsync.backend.dto.CreateTeamRequest;
import com.projectsync.backend.dto.TeamMemberResponse;
import com.projectsync.backend.dto.TeamResponse;
import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.exception.BadRequestException;
import com.projectsync.backend.exception.ConflictException;
import com.projectsync.backend.exception.ForbiddenOperationException;
import com.projectsync.backend.exception.ResourceNotFoundException;
import com.projectsync.backend.repository.TeamMemberRepository;
import com.projectsync.backend.repository.TeamRepository;
import com.projectsync.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository,
                        TeamMemberRepository teamMemberRepository,
                        UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeamResponse createTeam(Authentication authentication, CreateTeamRequest request) {
        User currentUser = resolveStudent(authentication);

        if (teamMemberRepository.existsByStudentUser(currentUser)) {
            throw new ConflictException("You already belong to a team.");
        }

        OffsetDateTime now = OffsetDateTime.now();

        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setCreatedBy(currentUser);
        team.setCreatedAt(now);
        team.setUpdatedAt(now);
        team = teamRepository.save(team);

        TeamMember leaderMembership = new TeamMember();
        leaderMembership.setTeam(team);
        leaderMembership.setStudentUser(currentUser);
        leaderMembership.setTeamRole("LEADER");
        leaderMembership.setJoinedAt(now);
        teamMemberRepository.save(leaderMembership);

        return toTeamResponse(team);
    }

    public TeamResponse getMyTeam(Authentication authentication) {
        User currentUser = resolveStudent(authentication);

        TeamMember membership = teamMemberRepository.findByStudentUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("You are not part of a team yet."));

        return toTeamResponse(membership.getTeam());
    }

    @Transactional
    public TeamMemberResponse addMember(Authentication authentication, UUID teamId, AddTeamMemberRequest request) {
        User currentUser = resolveStudent(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        TeamMember currentMembership = teamMemberRepository.findByStudentUser(currentUser)
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team."));

        if (!currentMembership.getTeam().getId().equals(teamId)) {
            throw new ForbiddenOperationException("You are not a member of this team.");
        }

        if (!"LEADER".equals(currentMembership.getTeamRole())) {
            throw new ForbiddenOperationException("Only the team leader can add members.");
        }

        User newStudent = userRepository.findById(request.getStudentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));

        if (!"STUDENT".equals(newStudent.getRole())) {
            throw new BadRequestException("The selected user is not a student.");
        }

        if (teamMemberRepository.existsByStudentUser(newStudent)) {
            throw new ConflictException("This student already belongs to a team.");
        }

        TeamMember newMembership = new TeamMember();
        newMembership.setTeam(team);
        newMembership.setStudentUser(newStudent);
        newMembership.setTeamRole("MEMBER");
        newMembership.setJoinedAt(OffsetDateTime.now());
        newMembership = teamMemberRepository.save(newMembership);

        return toMemberResponse(newMembership);
    }

    public List<TeamMemberResponse> getTeamMembers(Authentication authentication, UUID teamId) {
        User currentUser = resolveStudent(authentication);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        teamMemberRepository.findByTeamAndStudentUser(team, currentUser)
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team."));

        return teamMemberRepository.findByTeam(team).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    private User resolveStudent(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        if (!"STUDENT".equals(user.getRole())) {
            throw new ForbiddenOperationException("Only students can access this resource.");
        }

        return user;
    }

    private TeamResponse toTeamResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setTeamName(team.getTeamName());
        response.setCreatedBy(team.getCreatedBy().getId());
        response.setCreatedAt(team.getCreatedAt());
        response.setUpdatedAt(team.getUpdatedAt());
        return response;
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setId(member.getId());
        response.setStudentUserId(member.getStudentUser().getId());
        response.setFullName(member.getStudentUser().getFullName());
        response.setEmail(member.getStudentUser().getEmail());
        response.setTeamRole(member.getTeamRole());
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }
}