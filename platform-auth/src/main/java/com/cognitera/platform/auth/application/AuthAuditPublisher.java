package com.cognitera.platform.auth.application;

import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditMetadata;
import com.cognitera.platform.audit.api.AuditSubject;
import com.cognitera.platform.audit.api.AuthAuditEvents;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Publishes authentication-related audit events through the audit module. */
@Component
public class AuthAuditPublisher {

    private final AuthAuditEvents auditEvents;

    public AuthAuditPublisher(AuthAuditEvents auditEvents) {
        this.auditEvents = auditEvents;
    }

    /** Emits an authentication audit event with the given actor, event type, entity ID, and metadata. */
    public void emit(String actorId, AuditEventType eventType, String entityId, Map<String, String> metadata) {
        auditEvents.emit(
                eventType,
                AuditSubject.of(actorId, "AUTH_USER", entityId),
                AuditMetadata.from(metadata));
    }
}
