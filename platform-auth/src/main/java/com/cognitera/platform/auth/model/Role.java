package com.cognitera.platform.auth.model;

import java.util.Arrays;

/** User role enumeration with ADMIN, ANALYST, and USER levels. */
public enum Role {
    ADMIN,
    ANALYST,
    USER;

    /** Parses a role from a case-insensitive name string. */
    public static Role fromName(String value) {
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported role: " + value));
    }
}
