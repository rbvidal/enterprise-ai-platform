package com.cognitera.platform.api.dto.auth;

import com.cognitera.platform.auth.api.AuthTokens;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO containing authentication tokens and user info.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        UUID userId,
        String email,
        Set<String> roles
) {
    /**
     * Converts domain {@code AuthTokens} into an API response DTO.
     */
    public static AuthResponse from(AuthTokens tokens) {
        return new AuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.accessTokenExpiresAt(),
                tokens.refreshTokenExpiresAt(),
                tokens.userId(),
                tokens.email(),
                tokens.roles());
    }
}
