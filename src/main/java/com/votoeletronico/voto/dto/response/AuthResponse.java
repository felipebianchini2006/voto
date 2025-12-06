package com.votoeletronico.voto.dto.response;

import lombok.Builder;

/**
 * Authentication response DTO with JWT token
 */
@Builder
public record AuthResponse(
        String token,
        String type,
        UserResponse user
) {}
