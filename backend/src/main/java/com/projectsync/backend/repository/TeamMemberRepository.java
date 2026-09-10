package com.projectsync.backend.repository;

import com.projectsync.backend.entity.Team;
import com.projectsync.backend.entity.TeamMember;
import com.projectsync.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    Optional<TeamMember> findByStudentUser(User user);
    Optional<TeamMember> findByTeamAndStudentUser(Team team, User user);
    List<TeamMember> findByTeam(Team team);
    boolean existsByStudentUser(User user);
}