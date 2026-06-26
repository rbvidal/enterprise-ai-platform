package com.cognitera.platform.audit.infrastructure;

import com.cognitera.platform.audit.api.AiAuditEvents;
import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditService;
import com.cognitera.platform.audit.api.AuditSource;
import com.cognitera.platform.audit.api.AuditSubject;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/** Default implementation of {@link AiAuditEvents} sourcing from the "ai" module. */
@Component
public class DefaultAiAuditEvents implements AiAuditEvents {

    private final AuditService auditService;

    public DefaultAiAuditEvents(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void emit(AuditEventType eventType, AuditSubject subject, JsonNode metadata) {
        auditService.emit(eventType, subject, AuditSource.of("ai", metadata));
    }
}
