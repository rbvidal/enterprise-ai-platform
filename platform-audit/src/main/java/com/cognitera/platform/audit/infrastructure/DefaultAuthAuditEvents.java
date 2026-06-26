package com.cognitera.platform.audit.infrastructure;

import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditService;
import com.cognitera.platform.audit.api.AuditSource;
import com.cognitera.platform.audit.api.AuditSubject;
import com.cognitera.platform.audit.api.AuthAuditEvents;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** Default implementation of {@link AuthAuditEvents} sourcing from the "auth" module. */
@Component
public class DefaultAuthAuditEvents implements AuthAuditEvents {

    private final AuditService auditService;

    public DefaultAuthAuditEvents(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void emit(AuditEventType eventType, AuditSubject subject, JsonNode metadata) {
        auditService.emit(eventType, subject, AuditSource.of("auth", metadata));
    }
}
