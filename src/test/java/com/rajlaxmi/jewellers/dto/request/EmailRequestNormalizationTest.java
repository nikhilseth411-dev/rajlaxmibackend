package com.rajlaxmi.jewellers.dto.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailRequestNormalizationTest {

    @Test
    void trimsEmailBeforeValidationAcrossAuthRequests() {
        LoginRequest login = new LoginRequest();
        RegisterRequest register = new RegisterRequest();
        VerifyOtpRequest verifyOtp = new VerifyOtpRequest();
        ForgotPasswordRequest forgotPassword = new ForgotPasswordRequest();

        login.setEmail("  customer@example.com  ");
        register.setEmail("  customer@example.com  ");
        verifyOtp.setEmail("  customer@example.com  ");
        forgotPassword.setEmail("  customer@example.com  ");

        assertEquals("customer@example.com", login.getEmail());
        assertEquals("customer@example.com", register.getEmail());
        assertEquals("customer@example.com", verifyOtp.getEmail());
        assertEquals("customer@example.com", forgotPassword.getEmail());
    }
}
