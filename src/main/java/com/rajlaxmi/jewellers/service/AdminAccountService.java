package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.UpdateAdminCredentialsRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.UserResponse;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.enums.Role;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.exception.DuplicateResourceException;
import com.rajlaxmi.jewellers.repository.RefreshTokenRepository;
import com.rajlaxmi.jewellers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApiResponse<UserResponse> updateCredentials(
            User admin,
            UpdateAdminCredentialsRequest request) {

        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new BusinessException("Admin access is required.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new BusinessException("Current password is incorrect.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New passwords do not match.");
        }

        String normalizedEmail = request.getNewEmail().toLowerCase(Locale.ROOT).trim();
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, admin.getId())) {
            throw new DuplicateResourceException("User", "email", normalizedEmail);
        }

        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);
        refreshTokenRepository.revokeAllByUserId(admin.getId());

        return ApiResponse.success(
                "Admin credentials updated. Please login again.",
                toResponse(admin));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isEmailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
