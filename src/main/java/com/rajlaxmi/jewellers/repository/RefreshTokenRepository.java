package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * RefreshTokenRepository
 *
 * tokenHash: SHA-256 hash of the raw token sent to the client.
 * Client sends raw token → we hash it → findByTokenHash → validate.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Revoke all tokens for a user (logout all sessions)
    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    // Cleanup job: delete expired tokens older than 7 days
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);

    long countByUserIdAndIsRevokedFalse(Long userId);
}
