package com.votoeletronico.voto;

import com.votoeletronico.voto.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests.
 * Provides common configuration for all integration tests:
 * - Testcontainers for PostgreSQL
 * - Active test profile
 * - Transactional rollback between tests
 * - MockMvc auto-configuration
 * - Test security configuration with mock JWT decoder
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
@Transactional
@WithMockUser(roles = {"ADMIN", "OPERATOR", "AUDITOR"})
public abstract class BaseIntegrationTest {

    protected static final String TEST_JWT_TOKEN = "test-jwt-token";

    @BeforeEach
    void setUp() {
        // Common setup for all integration tests
        // Override in subclasses if needed
    }
}
