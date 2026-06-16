package com.rajlaxmi.jewellers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 * RefreshToken Entity — maps to 'refresh_tokens' table
 * ================================================================
 * WHY store refresh tokens in DB?
 *
 *   JWT access tokens are stateless — once issued, we can't revoke them
 *   until they expire (15 min). This is acceptable.
 *
 *   But refresh tokens (7 days) MUST be revocable:
 *   - User logs out → refresh token revoked immediately
 *   - Admin bans a user → all their refresh tokens revoked
 *   - Suspicious activity → all sessions can be killed
 *
 *   By storing refresh tokens in DB with an isRevoked flag,
 *   we can invalidate any session at any time.
 *
 * SECURITY:
 *   We store a hash of the token, not the raw token string.
 *   If the DB is compromised, raw tokens can't be extracted.
 *   When client sends refresh token → we hash it → compare with DB.
 *
 * TOKEN ROTATION:
 *   Every time a refresh token is used to get a new access token,
 *   the old refresh token is revoked and a new one is issued.
 *   This limits the window of exposure if a token is stolen.
 * ================================================================
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_user", columnList = "user_id"),
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SHA-256 hash of the actual token string.
     * The raw token is sent to the client and never stored.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean isRevoked = false;

    @Column(length = 45)
    private String ipAddress;   // IP when token was issued (audit)

    @Column(length = 200)
    private String userAgent;   // Browser/device info (audit)

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Helper ─────────────────────────────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isRevoked && !isExpired();
    }
}
