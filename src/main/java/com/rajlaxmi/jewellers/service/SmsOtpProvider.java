package com.rajlaxmi.jewellers.service;

public interface SmsOtpProvider {
    String sendOtp(String phone);
    boolean verifyOtp(String requestId, String otp);
}
