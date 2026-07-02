package com.rajlaxmi.jewellers.service.impl;

import com.rajlaxmi.jewellers.dto.request.PhoneOtpRequest;
import com.rajlaxmi.jewellers.dto.request.VerifyPhoneOtpRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.AuthResponse;
import com.rajlaxmi.jewellers.entity.PhoneOtpChallenge;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.repository.PhoneOtpChallengeRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import com.rajlaxmi.jewellers.service.AuthService;
import com.rajlaxmi.jewellers.service.PhoneAuthService;
import com.rajlaxmi.jewellers.service.SmsOtpProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneAuthServiceImpl implements PhoneAuthService {
    private final SmsOtpProvider smsOtpProvider;
    private final PhoneOtpChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:MSG91}")
    private String smsProvider;

    @Value("${app.sms.otp-expiry-minutes:10}")
    private long otpExpiryMinutes;

    @Override
    @Transactional
    public ApiResponse<String> requestOtp(PhoneOtpRequest request) {
        validateSmsConfiguration();
        challengeRepository.findTopByPhoneOrderByCreatedAtDesc(request.getPhone())
                .filter(challenge -> challenge.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1)))
                .ifPresent(challenge -> {
                    throw new BusinessException("Please wait one minute before requesting another OTP.");
                });
        String requestId = smsOtpProvider.sendOtp(request.getPhone());
        challengeRepository.save(PhoneOtpChallenge.builder()
                .phone(request.getPhone())
                .providerRequestId(requestId)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build());
        return ApiResponse.successMessage("OTP sent to your mobile number.");
    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> verifyOtp(VerifyPhoneOtpRequest request) {
        validateSmsConfiguration();
        PhoneOtpChallenge challenge = challengeRepository.findTopByPhoneOrderByCreatedAtDesc(request.getPhone())
                .orElseThrow(() -> new BusinessException("Please request a new OTP."));
        if (challenge.isVerified() || challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired. Please request a new OTP.");
        }
        if (challenge.getAttempts() >= 5) {
            throw new BusinessException("Too many invalid attempts. Please request a new OTP.");
        }

        boolean verified = smsOtpProvider.verifyOtp(challenge.getProviderRequestId(), request.getOtp());
        if (!verified) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            throw new BusinessException("Invalid OTP. Please try again.");
        }

        challenge.setVerified(true);
        challengeRepository.save(challenge);

        User user = userRepository.findByPhone(request.getPhone()).orElseGet(() -> createPhoneCustomer(request.getPhone()));
        user.setActive(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        return authService.createSessionForVerifiedUser(user.getId());
    }

    private User createPhoneCustomer(String phone) {
        return userRepository.save(User.builder()
                .firstName("Customer")
                .lastName(phone.substring(6))
                .email("customer." + phone + "@phone.rajlaxmi.local")
                .phone(phone)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CUSTOMER)
                .isEmailVerified(true)
                .isActive(true)
                .build());
    }

    private void validateSmsConfiguration() {
        if (!smsEnabled || !"MSG91".equalsIgnoreCase(smsProvider)) {
            throw new BusinessException("SMS OTP service is not configured yet.");
        }
    }
}
