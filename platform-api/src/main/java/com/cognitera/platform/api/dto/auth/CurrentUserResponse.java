package com.cognitera.platform.api.dto.auth;

import com.cognitera.platform.auth.api.AuthenticatedUser;

import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for the currently authenticated user's details.
 */
public record CurrentUserResponse(
        UUID id,
        String email,
        String displayName,
        Set<String> roles
) {
    /**
     * Converts an {@code AuthenticatedUser} domain object into an API response DTO.
     */
    public static CurrentUserResponse from(AuthenticatedUser user) {
        return new CurrentUserResponse(user.id(), user.email(), user.displayName(), user.roles());
    }
}
