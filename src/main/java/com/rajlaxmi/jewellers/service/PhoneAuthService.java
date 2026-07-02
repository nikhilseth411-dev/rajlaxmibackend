package com.rajlaxmi.jewellers.service;

import com.rajlaxmi.jewellers.dto.request.PhoneOtpRequest;
import com.rajlaxmi.jewellers.dto.request.VerifyPhoneOtpRequest;
import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import com.rajlaxmi.jewellers.dto.response.AuthResponse;

public interface PhoneAuthService {
    ApiResponse<String> requestOtp(PhoneOtpRequest request);
    ApiResponse<AuthResponse> verifyOtp(VerifyPhoneOtpRequest request);
}
