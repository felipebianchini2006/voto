package com.votoeletronico.voto.repository;

import com.votoeletronico.voto.domain.user.User;
import com.votoeletronico.voto.domain.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Handles database operations for admin and candidate users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by username and specific role
     */
    Optional<User> findByUsernameAndRole(String username, UserRole role);

    /**
     * Check if a user exists by username or email and is enabled
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM User u
            WHERE (u.username = :usernameOrEmail OR u.email = :usernameOrEmail)
            AND u.enabled = true
            """)
    boolean existsByUsernameOrEmailAndEnabled(String usernameOrEmail);
}
