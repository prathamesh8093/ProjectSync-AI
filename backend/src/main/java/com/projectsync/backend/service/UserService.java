package com.projectsync.backend.service;

import com.projectsync.backend.dto.RegisterRequest;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.projectsync.backend.exception.ConflictException(
                    "Email is already registered"
            );
        }

        String role = request.getRole().toUpperCase();

        if (!role.equals("STUDENT") && !role.equals("FACULTY")) {
            throw new com.projectsync.backend.exception.BadRequestException(
                    "Role must be STUDENT or FACULTY"
            );
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(role);
        user.setIsActive(true);

        OffsetDateTime now = OffsetDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> searchStudents(String query) {
        return userRepository.searchStudents(query);
    }
}