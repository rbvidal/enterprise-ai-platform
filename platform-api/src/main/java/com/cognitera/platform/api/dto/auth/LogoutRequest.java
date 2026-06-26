package com.cognitera.platform.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO containing the refresh token to be invalidated.
 */
public record LogoutRequest(
        @NotBlank String refreshToken
) {
}
