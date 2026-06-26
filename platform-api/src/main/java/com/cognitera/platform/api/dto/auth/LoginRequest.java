package com.cognitera.platform.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user login credentials.
 */
public record LoginRequest(
        @Email @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(min = 3, max = 128) String password
) {
}
