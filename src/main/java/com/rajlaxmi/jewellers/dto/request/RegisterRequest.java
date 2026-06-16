package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ================================================================
 * RegisterRequest — POST /auth/register
 * ================================================================
 * Validation annotations here serve as the first line of defense:
 *   @NotBlank: field must not be null or empty/whitespace
 *   @Email: must match valid email pattern
 *   @Size: enforces min/max length constraints
 *   @Pattern: regex for phone number format
 *
 * These trigger when the controller uses @Valid.
 * Invalid requests are caught by GlobalExceptionHandler
 * and returned as 400 Bad Request with field-level error details.
 *
 * Password strength: min 8 chars, at least one uppercase,
 * one lowercase, one digit, one special character.
 * ================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150)
    private String email;

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Please provide a valid 10-digit Indian mobile number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    private String password;
}
