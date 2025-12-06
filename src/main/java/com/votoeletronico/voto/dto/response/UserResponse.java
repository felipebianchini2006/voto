package com.votoeletronico.voto.dto.response;

import com.votoeletronico.voto.domain.user.UserRole;
import java.time.Instant;
import java.util.UUID;

/**
 * User response DTO
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        Boolean enabled,
        Instant lastLoginAt,
        Instant createdAt
) {}
