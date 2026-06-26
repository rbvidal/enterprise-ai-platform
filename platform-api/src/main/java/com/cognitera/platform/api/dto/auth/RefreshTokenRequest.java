package com.cognitera.platform.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO containing a refresh token for obtaining new access tokens.
 */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
