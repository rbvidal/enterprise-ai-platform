package com.cognitera.platform.audit.api;

import java.time.Instant;

/** Query record for filtering and paginating audit events with validation on page and size bounds. */
public record AuditQuery(
        AuditEventType eventType,
        String actorId,
        String tenantId,
        String entityType,
        String entityId,
        String sourceModule,
        String correlationId,
        String requestId,
        Instant from,
        Instant to,
        int page,
        int size
) {
    public AuditQuery {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 200);
    }
}
