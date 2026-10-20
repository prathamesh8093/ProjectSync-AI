package com.projectsync.backend.service;

import com.projectsync.backend.dto.AuthResponse;
import com.projectsync.backend.dto.LoginRequest;
import com.projectsync.backend.dto.RegisterRequest;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        User user = userService.registerUser(request);

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {

        User user = userService
                .getUserByEmail(request.getEmail())
                .orElseThrow(() ->
                        new org.springframework.security.authentication.BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new com.projectsync.backend.exception.ForbiddenOperationException(
                    "User account is inactive"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}