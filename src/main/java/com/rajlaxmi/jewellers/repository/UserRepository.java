package com.rajlaxmi.jewellers.repository;

import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ================================================================
 * UserRepository
 * ================================================================
 * Extends JpaRepository which provides:
 *   save(), findById(), findAll(), delete(), count(), existsById()
 *   + pagination + sorting — all out of the box.
 *
 * Custom queries use JPQL (Java Persistence Query Language),
 * not raw SQL, so they work with any JPA-compliant DB.
 *
 * WHY @Modifying on update queries?
 *   Without it, Spring Data JPA will treat the query as a SELECT
 *   and throw an exception for UPDATE/DELETE operations.
 *   @Transactional is required on the SERVICE layer, not here.
 * ================================================================
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Auth Queries ──────────────────────────────────────────

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhone(String phone);

    // ── OTP Verification ──────────────────────────────────────

    Optional<User> findByEmailOtpAndEmail(String otp, String email);

    @Modifying
    @Query("UPDATE User u SET u.isEmailVerified = true, u.emailOtp = null, " +
           "u.otpExpiresAt = null WHERE u.email = :email")
    void verifyEmail(@Param("email") String email);

    // ── Password Reset ────────────────────────────────────────

    Optional<User> findByPasswordResetToken(String token);

    // ── Brute Force / Account Locking ─────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 " +
           "WHERE u.email = :email")
    void incrementFailedAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.email = :email")
    void resetFailedAttempts(@Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.lockedUntil = :lockedUntil WHERE u.email = :email")
    void lockAccount(@Param("email") String email,
                     @Param("lockedUntil") LocalDateTime lockedUntil);

    // ── Admin Dashboard Queries ────────────────────────────────

    long countByRole(Role role);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);
}
