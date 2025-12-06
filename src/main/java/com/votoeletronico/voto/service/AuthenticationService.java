package com.votoeletronico.voto.service;

import com.votoeletronico.voto.domain.user.User;
import com.votoeletronico.voto.domain.user.UserRole;
import com.votoeletronico.voto.dto.request.CandidateRegistrationRequest;
import com.votoeletronico.voto.dto.request.LoginRequest;
import com.votoeletronico.voto.dto.response.AuthResponse;
import com.votoeletronico.voto.dto.response.UserResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.security.JwtTokenProvider;
import com.votoeletronico.voto.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service for login and registration
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Authenticate user and generate JWT token
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            User user = (User) authentication.getPrincipal();
            String token = tokenProvider.generateToken(user);

            userService.recordLogin(user.getUsername(), true);

            log.info("User logged in successfully: {}", user.getUsername());

            return AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userMapper.toResponse(user))
                    .build();

        } catch (AuthenticationException e) {
            userService.recordLogin(request.username(), false);
            log.warn("Login failed for username: {}", request.username());
            throw new BusinessException("Invalid username or password");
        }
    }

    /**
     * Register new candidate
     */
    @Transactional
    public AuthResponse registerCandidate(CandidateRegistrationRequest request) {
        log.info("Candidate registration for username: {}", request.username());

        // Create user with CANDIDATE role
        UserResponse userResponse = userService.createUser(
                request.username(),
                request.password(),
                request.email(),
                UserRole.CANDIDATE
        );

        // Get full user object for token generation
        User user = userService.getUserById(userResponse.id());
        String token = tokenProvider.generateToken(user);

        log.info("Candidate registered successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    /**
     * Get current authenticated user
     */
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("Not authenticated");
        }
        return (User) authentication.getPrincipal();
    }
}
