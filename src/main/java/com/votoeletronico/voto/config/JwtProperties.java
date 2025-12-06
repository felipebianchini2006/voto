package com.votoeletronico.voto.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 *
 * Configuration via application.yml under app.jwt prefix.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret key for JWT signing (min 256 bits for HS256).
     * MUST be set via environment variable JWT_SECRET in production.
     */
    private String secret = "change-this-secret-in-production-must-be-at-least-256-bits-long";

    /**
     * Token expiration time in milliseconds (default: 24 hours)
     */
    private long expirationMs = 86400000; // 24 hours

    /**
     * Refresh token expiration time in milliseconds (default: 7 days)
     */
    private long refreshExpirationMs = 604800000; // 7 days

    /**
     * JWT issuer claim
     */
    private String issuer = "voto-system";
}
