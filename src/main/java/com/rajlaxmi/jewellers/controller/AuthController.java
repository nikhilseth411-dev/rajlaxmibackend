package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.*;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.AuthResponse;
import com.rajlaxmi.jewellers.service.AuthService;
import com.rajlaxmi.jewellers.service.PhoneAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — /auth/**
 * All endpoints are PUBLIC (no JWT required) — configured in SecurityConfig.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, OTP, Token Refresh, Password Reset")
public class AuthController {

    private final AuthService authService;
    private final PhoneAuthService phoneAuthService;

    @PostMapping("/register")
    @Operation(summary = "Register new customer account")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify email OTP and activate account")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP if expired")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendOtp(email));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        String token = request != null ? request.getRefreshToken() : null;
        return ResponseEntity.ok(authService.logout(token));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset link to email")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token from email")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/phone/request-otp")
    @Operation(summary = "Send customer login OTP to an Indian mobile number")
    public ResponseEntity<ApiResponse<String>> requestPhoneOtp(@Valid @RequestBody PhoneOtpRequest request) {
        return ResponseEntity.ok(phoneAuthService.requestOtp(request));
    }

    @PostMapping("/phone/verify-otp")
    @Operation(summary = "Verify mobile OTP and create or login the customer")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyPhoneOtp(@Valid @RequestBody VerifyPhoneOtpRequest request) {
        return ResponseEntity.ok(phoneAuthService.verifyOtp(request));
    }
}
