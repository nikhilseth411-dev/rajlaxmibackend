package com.rajlaxmi.jewellers.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse — returned by /auth/login and /auth/register (after OTP verify)
 *
 * Contains:
 *   accessToken: JWT token, valid 15 minutes. Frontend stores in memory
 *                (NOT localStorage — XSS risk). Used in Authorization header.
 *   refreshToken: valid 7 days. Frontend stores in httpOnly cookie OR
 *                 secure storage. Used to get new access token.
 *   tokenType: always "Bearer" — standard OAuth2 format
 *   user: basic user info for the UI (name, email, role)
 *
 * SECURITY NOTE FOR FRONTEND:
 *   Store accessToken in React state or Redux (memory only).
 *   Store refreshToken in httpOnly cookie (server-set) for best security.
 *   Never store either token in localStorage — vulnerable to XSS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long accessTokenExpiresIn; // milliseconds

    private UserResponse user;
}
