package com.cognitera.platform.api.dto.audit;

import com.cognitera.platform.audit.api.AuditEvent;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a single audit event.
 */
public record AuditEventResponse(
        UUID id,
        Instant timestamp,
        String actorId,
        String tenantId,
        String eventType,
        String entityType,
        String entityId,
        String sourceModule,
        String correlationId,
        String requestId,
        String requestPath,
        String httpMethod,
        JsonNode metadata
) {
    /**
     * Converts an {@code AuditEvent} domain object into an API response DTO.
     */
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.id(),
                event.timestamp(),
                event.actorId(),
                event.tenantId(),
                event.eventType().name(),
                event.entityType(),
                event.entityId(),
                event.sourceModule(),
                event.correlationId(),
                event.requestId(),
                event.requestPath(),
                event.httpMethod(),
                event.metadata());
    }
}
