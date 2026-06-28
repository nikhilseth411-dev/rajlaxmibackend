package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** POST /auth/verify-otp — verifies email OTP sent on registration */
@Data
public class VerifyOtpRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
    private String otp;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
