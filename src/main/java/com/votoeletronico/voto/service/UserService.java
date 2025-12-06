package com.votoeletronico.voto.service;

import com.votoeletronico.voto.domain.user.User;
import com.votoeletronico.voto.domain.user.UserRole;
import com.votoeletronico.voto.dto.response.UserResponse;
import com.votoeletronico.voto.exception.BusinessException;
import com.votoeletronico.voto.exception.ResourceNotFoundException;
import com.votoeletronico.voto.repository.UserRepository;
import com.votoeletronico.voto.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User management service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Create a new user (admin or candidate)
     */
    @Transactional
    public UserResponse createUser(String username, String password, String email, UserRole role) {
        log.info("Creating new user: {}", username);

        // Validate uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already exists: " + email);
        }

        // Validate password strength
        validatePassword(password);

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .role(role)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created successfully: {}", saved.getId());

        return userMapper.toResponse(saved);
    }

    /**
     * Get user by ID
     */
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Record login attempt
     */
    @Transactional
    public void recordLogin(String username, boolean success) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (success) {
                user.recordSuccessfulLogin();
            } else {
                user.recordFailedLogin();
            }
            userRepository.save(user);
        });
    }

    /**
     * Validate password strength
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("Password must contain at least one digit");
        }
    }
}
