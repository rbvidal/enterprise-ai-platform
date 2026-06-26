package com.cognitera.platform.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request DTO for new user registration.
 */
public record RegisterRequest(
        @Email @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(min = 3, max = 128) String password,
        @NotBlank @Size(max = 255) String displayName,
        Set<String> roles
) {
}
