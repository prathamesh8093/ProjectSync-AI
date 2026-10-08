package com.projectsync.backend.config;

import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Initializes default test credentials for standalone local development
 * (Profile: "local").
 * Never executes in production or 'supabase' profile.
 */
@Component
@Profile("local")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUserIfNotExists("student@projectsync.local", "Password123!", "Local Student", "STUDENT");
        seedUserIfNotExists("faculty@projectsync.local", "Password123!", "Local Faculty", "FACULTY");
    }

    private void seedUserIfNotExists(String email, String rawPassword, String fullName, String role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setRole(role);
            user.setIsActive(true);
            OffsetDateTime now = OffsetDateTime.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);
            log.info("Seeded local development user: {} [{}]", email, role);
        }
    }
}
