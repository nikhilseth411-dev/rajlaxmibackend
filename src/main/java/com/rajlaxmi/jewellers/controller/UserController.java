package com.rajlaxmi.jewellers.controller;

import com.rajlaxmi.jewellers.dto.request.ChangePasswordRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateProfileRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.UserResponse;
import com.rajlaxmi.jewellers.entity.User;
import com.rajlaxmi.jewellers.exception.BusinessException;
import com.rajlaxmi.jewellers.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.rajlaxmi.jewellers.enums.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Manage your account and profile")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    @Operation(summary = "Get current user pro  file")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(toResponse(user)));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile name and phone")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully.", toResponse(user)));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password while logged in")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect.");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully. Please login again."));
    }
    @GetMapping("/admin/customers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get registered customers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<UserResponse> customers = userRepository
                .findByRole(Role.CUSTOMER, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId()).firstName(u.getFirstName()).lastName(u.getLastName())
                .email(u.getEmail()).phone(u.getPhone()).role(u.getRole())
                .isEmailVerified(u.isEmailVerified()).isActive(u.isActive())
                .createdAt(u.getCreatedAt()).build();
    }
}
