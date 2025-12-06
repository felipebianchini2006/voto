package com.votoeletronico.voto.config;

import com.votoeletronico.voto.domain.user.UserRole;
import com.votoeletronico.voto.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default system data on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("DataInitializer: Starting initialization...");
        try {
            // Try to get existing admin user
            log.info("DataInitializer: Checking if admin1 exists...");
            userService.getUserByUsername("admin1");
            log.info("Admin user 'admin1' already exists, skipping initialization");
        } catch (Exception e) {
            // User doesn't exist, create it
            log.info("DataInitializer: admin1 not found, creating new user");
            try {
                log.info("Creating default admin user 'admin1'");
                userService.createUser(
                    "admin1",
                    "Admin1234",  // Meets password requirements: 8+ chars, uppercase, lowercase, digit
                    "admin1@voto.local",
                    UserRole.ADMIN
                );
                log.info("✓ Default admin user created successfully - Username: admin1 | Password: Admin1234");
            } catch (Exception ex) {
                log.error("Failed to create default admin user: {}", ex.getMessage(), ex);
            }
        }
    }
}
