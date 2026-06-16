package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.ChangePasswordRequest;
import com.rajlaxmi.jewellers.dto.request.UpdateProfileRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.PagedResponse;
import com.rajlaxmi.jewellers.dto.response.UserResponse;

public interface UserService {

    UserResponse getProfile(Long userId);

    ApiResponse<UserResponse> updateProfile(Long userId, UpdateProfileRequest request);

    ApiResponse<String> changePassword(Long userId, ChangePasswordRequest request);

    ApiResponse<String> deactivateAccount(Long userId);

    // Admin
    PagedResponse<UserResponse> getAllUsers(int page, int size, String search);

    ApiResponse<String> adminToggleUserStatus(Long targetUserId, boolean active);
}
