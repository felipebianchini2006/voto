package com.votoeletronico.voto.domain.user;

import com.votoeletronico.voto.domain.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

/**
 * User entity representing authenticated users (Admins and Candidates).
 *
 * Implements Spring Security's UserDetails interface for authentication.
 *
 * Note: Voters are NOT users - they remain passwordless for privacy preservation.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "account_expired", nullable = false)
    @Builder.Default
    private Boolean accountExpired = false;

    @Column(name = "credentials_expired", nullable = false)
    @Builder.Default
    private Boolean credentialsExpired = false;

    @Column(name = "password_change_required", nullable = false)
    @Builder.Default
    private Boolean passwordChangeRequired = false;

    @Column(name = "last_password_change_at")
    private Instant lastPasswordChangeAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_at")
    private Instant lockedAt;

    // ============================================================================
    // UserDetails Implementation
    // ============================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !Boolean.TRUE.equals(accountExpired);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !Boolean.TRUE.equals(accountLocked);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !Boolean.TRUE.equals(credentialsExpired);
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    // ============================================================================
    // Business Methods
    // ============================================================================

    /**
     * Check if user is an administrator
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Check if user is a candidate
     */
    public boolean isCandidate() {
        return role == UserRole.CANDIDATE;
    }

    /**
     * Record successful login - resets failed attempts and updates login timestamp
     */
    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginAttempts = 0;
    }

    /**
     * Record failed login attempt - locks account after 5 failures
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
            this.lockedAt = Instant.now();
        }
    }

    /**
     * Unlock account and reset failed login attempts
     */
    public void unlockAccount() {
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
        this.lockedAt = null;
    }

    /**
     * Change password and update metadata
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.lastPasswordChangeAt = Instant.now();
        this.passwordChangeRequired = false;
    }
}
