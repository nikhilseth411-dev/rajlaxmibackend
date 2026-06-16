package com.rajlaxmi.jewellers.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ================================================================
 * JwtUtil — JWT Token Generation and Validation
 * ================================================================
 * WHAT IS JWT?
 *   JSON Web Token — a compact, URL-safe token with 3 parts:
 *   Header.Payload.Signature (Base64-encoded, dot-separated)
 *
 *   Header: {"alg": "HS256", "typ": "JWT"}
 *   Payload: {"sub": "user@email.com", "role": "CUSTOMER", "iat": ..., "exp": ...}
 *   Signature: HMAC-SHA256(header + "." + payload, secretKey)
 *
 * WHY TWO TOKENS?
 *   Access Token  (15 min): short-lived, sent with every API request.
 *                           If stolen, attacker access expires in 15 min.
 *   Refresh Token (7 days): used ONLY to get a new access token.
 *                           Stored in DB so we can revoke it.
 *
 * SIGNING ALGORITHM: HS256 (HMAC with SHA-256)
 *   - Symmetric: same secret for signing and verification
 *   - Good for single-backend systems (no need for asymmetric RS256)
 *   - Secret must be at least 256 bits (32 characters) long
 *
 * SECRET KEY:
 *   Read from application.yml → environment variable.
 *   NEVER hardcoded. NEVER committed to git.
 * ================================================================
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    /**
     * Generates the signing key from the secret string.
     * Called on each use — key is derived, not stored as field,
     * to avoid accidental serialization.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token Generation ──────────────────────────────────────

    /**
     * Creates a JWT access token for the authenticated user.
     *
     * Claims included:
     *   - sub (subject): user's email — the unique identifier
     *   - role: user's role for RBAC enforcement in security filter
     *   - userId: for quick lookup without a DB query
     *   - iat (issued at): timestamp
     *   - exp (expiry): now + 15 minutes
     */
    public String generateAccessToken(UserDetails userDetails, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())  // email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token Parsing ─────────────────────────────────────────

    /**
     * Extracts all claims from a token.
     * Throws JwtException if token is tampered or expired.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ── Token Validation ──────────────────────────────────────

    /**
     * Validates the token:
     *   1. Signature matches (not tampered)
     *   2. Token not expired
     *   3. Subject matches the UserDetails email
     *
     * Called by JwtAuthenticationFilter on every request.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Quick check for just the token structure and signature
     * without checking expiry. Used for token refresh flow.
     */
    public boolean isTokenStructureValid(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
