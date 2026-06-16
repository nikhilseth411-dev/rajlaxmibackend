package com.rajlaxmi.jewellers.service;

public interface EmailService {
    void sendOtpEmail(String to, String name, String otp);
    void sendPasswordResetEmail(String to, String name, String resetToken);
    void sendWelcomeEmail(String to, String name);
}
