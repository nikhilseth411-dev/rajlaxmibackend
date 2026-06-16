package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.*;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.AuthResponse;

/**
 * ================================================================
 * AuthService — Authentication Business Logic Interface
 * ================================================================
 * Defines the contract for all authentication operations.
 *
 * WHY use an interface?
 *   1. Dependency Inversion Principle — controllers depend on the
 *      interface, not the implementation. Easier to swap impl.
 *   2. Easier to mock in unit tests (Mockito.mock(AuthService.class))
 *   3. Shows good enterprise architecture patterns in interviews
 *
 * IMPLEMENTATION: AuthServiceImpl.java
 * ================================================================
 */
public interface AuthService {

    /** Register new customer → sends OTP to email → returns pending message */
    ApiResponse<String> register(RegisterRequest request);

    /** Verify email OTP after registration → activates account → returns JWT */
    ApiResponse<AuthResponse> verifyOtp(VerifyOtpRequest request);

    /** Resend OTP if the previous one expired */
    ApiResponse<String> resendOtp(String email);

    /** Login → validates credentials → returns access + refresh tokens */
    ApiResponse<AuthResponse> login(LoginRequest request);

    /** Refresh access token using valid refresh token */
    ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request);

    /** Logout → revokes refresh token → clears session */
    ApiResponse<String> logout(String refreshToken);

    /** Send password reset OTP to email */
    ApiResponse<String> forgotPassword(ForgotPasswordRequest request);

    /** Reset password using token received via email */
    ApiResponse<String> resetPassword(ResetPasswordRequest request);
}
