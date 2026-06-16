package com.rajlaxmi.jewellers.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /auth/refresh — exchanges refresh token for new access token */
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
