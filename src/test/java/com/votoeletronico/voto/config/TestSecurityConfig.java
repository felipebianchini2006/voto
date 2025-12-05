package com.votoeletronico.voto.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Test security configuration that provides a mock JwtDecoder
 * for integration tests that don't use a real OAuth2 provider.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Mock JwtDecoder that creates a valid JWT for any token.
     * This allows tests to use mock JWT tokens without a real OAuth2 provider.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> {
            // For tests, create a mock JWT with admin roles
            Instant now = Instant.now();
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .header("typ", "JWT")
                    .claim("sub", "test-user")
                    .claim("roles", List.of("ADMIN", "OPERATOR", "AUDITOR"))
                    .claim("iss", "test-issuer")
                    .claim("aud", "test-audience")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600))
                    .build();
        };
    }
}
