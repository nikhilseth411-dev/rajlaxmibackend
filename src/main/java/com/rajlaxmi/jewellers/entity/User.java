package com.rajlaxmi.jewellers.entity;

import com.rajlaxmi.jewellers.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ================================================================
 * User Entity — maps to 'users' table
 * ================================================================
 * WHY implements UserDetails?
 *   Spring Security requires the authenticated principal to implement
 *   UserDetails. By implementing it directly on our entity, we avoid
 *   a separate UserDetailsImpl wrapper class, keeping code DRY.
 *   Spring Security calls getAuthorities(), isAccountNonLocked(), etc.
 *   directly on this object during authentication.
 *
 * SECURITY DESIGN DECISIONS:
 *   - password is BCrypt-hashed (strength 12) — NEVER stored as plaintext
 *   - isEmailVerified: account is inactive until email OTP is verified
 *   - failedLoginAttempts + lockedUntil: brute-force protection
 *     After 5 failed logins → account locked for 30 minutes
 *   - isActive: admin can manually disable a user account
 *
 * AUDIT FIELDS (createdAt, updatedAt):
 *   @CreationTimestamp / @UpdateTimestamp automatically set by Hibernate
 *   No need to manually set these in service layer.
 * ================================================================
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_phone", columnList = "phone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Personal Info ────────────────────────────────────────
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 15)
    private String phone;

    // ── Security Fields ───────────────────────────────────────
    /**
     * BCrypt hashed password. Never expose in API responses.
     * Strength 12 = ~250ms per hash = strong brute-force resistance.
     */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    // ── Email Verification ────────────────────────────────────
    /**
     * Account is disabled until email is verified via OTP.
     * isEnabled() returns this value — Spring Security blocks
     * unverified users from authenticating.
     */
    @Builder.Default
    private boolean isEmailVerified = false;

    @Column(length = 6)
    private String emailOtp;                // 6-digit OTP

    private LocalDateTime otpExpiresAt;     // OTP expires in 10 minutes

    // ── Brute Force Protection ────────────────────────────────
    /**
     * Tracks consecutive failed login attempts.
     * Reset to 0 on successful login.
     * When it reaches 5, lockedUntil is set to now() + 30 min.
     */
    @Builder.Default
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;      // null = not locked

    // ── Admin Control ─────────────────────────────────────────
    @Builder.Default
    private boolean isActive = true;        // admin can deactivate

    // ── Password Reset ────────────────────────────────────────
    private String passwordResetToken;
    private LocalDateTime passwordResetExpiresAt;

    // ── Audit ─────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Spring Security UserDetails Implementation ────────────

    /**
     * Maps our Role enum to Spring Security GrantedAuthority.
     * Prefix "ROLE_" is required for hasRole() checks to work.
     * e.g. Role.ADMIN → "ROLE_ADMIN"
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email; // email is our unique identifier
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Account is locked if lockedUntil is in the future.
     * Spring Security will reject login with AccountLockedException.
     */
    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return isActive && isEmailVerified;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ── Helper Methods ────────────────────────────────────────
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
