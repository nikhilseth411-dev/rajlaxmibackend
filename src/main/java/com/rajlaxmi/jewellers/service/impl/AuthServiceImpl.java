package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.*;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.AuthResponse;
import com.rajlaxmi.jewellers.dto.response.UserResponse;
import com.rajlaxmi.jewellers.entity.RefreshToken;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.DuplicateResourceException;
import com.rajlaxmi.jewellers.exception.ResourceNotFoundException;
import com.rajlaxmi.jewellers.repository.RefreshTokenRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.AuthService;
import com.rajlaxmi.jewellers.service.EmailService;
import com.rajlaxmi.jewellers.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * AuthServiceImpl — Complete Authentication Implementation
 * ================================================================
 * REGISTRATION FLOW:
 *   1. Check email not already registered
 *   2. Hash password with BCrypt
 *   3. Generate 6-digit OTP
 *   4. Save user with isEmailVerified = false
 *   5. Send OTP email
 *   6. Return "Check your email" message
 *
 * OTP VERIFICATION FLOW:
 *   1. Find user by email + OTP
 *   2. Check OTP not expired (10 minutes)
 *   3. Mark email as verified
 *   4. Generate access + refresh tokens
 *   5. Return AuthResponse with tokens
 *
 * LOGIN FLOW:
 *   1. Check account not locked
 *   2. AuthenticationManager authenticates (BCrypt compare)
 *   3. On success: reset failed attempts, generate tokens
 *   4. On failure: increment failed attempts, lock if >= 5
 *
 * REFRESH TOKEN FLOW:
 *   1. Find refresh token in DB by SHA-256 hash
 *   2. Check not expired, not revoked
 *   3. Revoke old refresh token (rotation)
 *   4. Generate new access + refresh token pair
 *   5. Return new AuthResponse
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${rate-limit.login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${rate-limit.lockout-duration-min:30}")
    private int lockoutDurationMin;

    @Value("${rate-limit.otp-expiry-min:10}")
    private int otpExpiryMin;

    @Value("${auth.expose-otp-in-response:false}")
    private boolean exposeOtpInResponse;

    private final SecureRandom secureRandom = new SecureRandom();

    // ── Registration ──────────────────────────────────────────

    @Override
    public ApiResponse<String> register(RegisterRequest request) {
        // 1. Check duplicate email
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // 2. Generate OTP
        String otp = generateOtp();

        // 3. Create user (unverified)
        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .isEmailVerified(false)
                .emailOtp(otp)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMin))
                .build();

        userRepository.save(user);

        // 4. Send OTP email (async in production)
        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp);

        log.info("User registered: {}. OTP sent to email.", user.getEmail());

        String message = "Registration successful! Please check your email for the 6-digit OTP to verify your account.";
        if (exposeOtpInResponse) {
            message += " Dev OTP: " + otp;
        }
        return ApiResponse.successMessage(message);
    }

    // ── OTP Verification ──────────────────────────────────────

    @Override
    public ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check OTP match
        if (!request.getOtp().equals(user.getEmailOtp())) {
            throw new BusinessException("Invalid OTP. Please check your email and try again.");
        }

        // Check OTP expiry
        if (user.getOtpExpiresAt() == null || LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }

        // Mark verified, clear OTP
        userRepository.verifyEmail(user.getEmail());
        user.setEmailVerified(true);
        user.setEmailOtp(null);
        user.setOtpExpiresAt(null);

        // Generate tokens
        AuthResponse authResponse = generateAuthResponse(user);

        log.info("Email verified for user: {}", user.getEmail());
        return ApiResponse.success("Email verified successfully! Welcome to राज लक्ष्मी ज्वेलर्स!", authResponse);
    }

    @Override
    public ApiResponse<String> resendOtp(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.isEmailVerified()) {
            throw new BusinessException("Email is already verified.");
        }

        String otp = generateOtp();
        user.setEmailOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMin));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp);
        String message = "New OTP sent to " + email;
        if (exposeOtpInResponse) {
            message += ". Dev OTP: " + otp;
        }
        return ApiResponse.successMessage(message);
    }

    // ── Login ─────────────────────────────────────────────────

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        // Load user first to check lock status BEFORE authentication
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with this email. Please register first."));

        // A completed lockout starts a fresh attempt window.
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(LocalDateTime.now())) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        // Check account lock
        if (!user.isAccountNonLocked()) {
            throw new BusinessException(
                    "Account is temporarily locked. Try again after " + lockoutDurationMin + " minutes.");
        }

        try {
            // Spring Security authenticates (BCrypt compare)
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            User authenticatedUser = (User) auth.getPrincipal();
            authenticatedUser.setFailedLoginAttempts(0);
            authenticatedUser.setLockedUntil(null);
            userRepository.save(authenticatedUser);

            AuthResponse response = generateAuthResponse(authenticatedUser);

            log.info("User logged in: {}", email);
            return ApiResponse.success("Login successful! Welcome back, " + authenticatedUser.getFirstName() + ".", response);

        } catch (BadCredentialsException e) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= maxLoginAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMin));
                userRepository.save(user);
                throw new BusinessException(
                        "Too many failed attempts. Account locked for " + lockoutDurationMin + " minutes.");
            }

            userRepository.save(user);
            int remaining = maxLoginAttempts - attempts;
            throw new BusinessException(
                    "Invalid email or password. " + remaining + " attempt(s) remaining before account lock.");
        }
    }

    // ── Token Refresh ─────────────────────────────────────────

    @Override
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token. Please login again."));

        if (!refreshToken.isValid()) {
            throw new BusinessException("Refresh token has expired or been revoked. Please login again.");
        }

        // Rotate: revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new pair
        User user = refreshToken.getUser();
        AuthResponse response = generateAuthResponse(user);

        return ApiResponse.success(response);
    }

    // ── Logout ────────────────────────────────────────────────

    @Override
    public ApiResponse<String> logout(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            String tokenHash = hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    });
        }
        return ApiResponse.successMessage("Logged out successfully.");
    }

    // ── Password Reset ────────────────────────────────────────

    @Override
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) {
        final String[] devOtp = new String[1];

        userRepository.findByEmail(request.getEmail().toLowerCase().trim()).ifPresent(user -> {
            String otp = generateOtp();

            user.setPasswordResetToken(otp);
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMin));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), otp);

            devOtp[0] = otp;
            log.info("Password reset OTP generated for user: {}", user.getEmail());
        });

        String message = "If an account with this email exists, a password reset OTP has been sent.";

        if (exposeOtpInResponse && devOtp[0] != null) {
            message += " Dev OTP: " + devOtp[0];
        }

        return ApiResponse.successMessage(message);
    }

    @Override
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired OTP."));

        if (user.getPasswordResetExpiresAt() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetExpiresAt())) {
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Revoke all refresh tokens for this user (force re-login on all devices)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return ApiResponse.successMessage("Password reset successfully. Please login with your new password.");
    }

    @Override
    public ApiResponse<AuthResponse> createSessionForVerifiedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!user.isActive() || user.getRole() != Role.CUSTOMER) {
            throw new BusinessException("This customer account is not available.");
        }
        return ApiResponse.success("Phone verified successfully.", generateAuthResponse(user));
    }

    // ── Private Helpers ───────────────────────────────────────

    /**
     * Generates access token + refresh token and saves refresh token to DB.
     * Called after successful login, OTP verification, and token refresh.
     */
    private AuthResponse generateAuthResponse(User user) {
        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(user, user.getId(), user.getRole().name());

        // Generate raw refresh token (UUID — 36 chars, then additional entropy)
        String rawRefreshToken = UUID.randomUUID() + "-" + System.currentTimeMillis();

        // Store HASH of refresh token (never store raw token)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpiryMs * 1_000_000L))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)  // raw token sent to client
                .tokenType("Bearer")
                .accessTokenExpiresIn(accessTokenExpiryMs)
                .user(UserResponse.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .isEmailVerified(user.isEmailVerified())
                        .isActive(user.isActive())
                        .build())
                .build();
    }

    /** Generates a 6-digit numeric OTP */
    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(999999));
    }

    /**
     * SHA-256 hashes the raw token for secure DB storage.
     * Client sends raw token → we hash it → compare with stored hash.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Token hashing failed", e);
        }
    }
}
