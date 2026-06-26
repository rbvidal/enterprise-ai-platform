package com.cognitera.platform.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repository for {@link RefreshTokenSessionEntity} with token-hash lookup. */
public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSessionEntity, UUID> {
    /** Finds a session by the hashed refresh token value. */
    Optional<RefreshTokenSessionEntity> findByTokenHash(String tokenHash);
}
