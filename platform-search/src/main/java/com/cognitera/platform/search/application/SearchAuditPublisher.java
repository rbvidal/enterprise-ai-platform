package com.cognitera.platform.search.application;

import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditMetadata;
import com.cognitera.platform.audit.api.AuditSubject;
import com.cognitera.platform.audit.api.SearchAuditEvents;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Publishes search-related audit events through the audit module. */
@Component
public class SearchAuditPublisher {

    private final SearchAuditEvents auditEvents;

    public SearchAuditPublisher(SearchAuditEvents auditEvents) {
        this.auditEvents = auditEvents;
    }

    /** Emits a search audit event with actor, tenant, event type, entity ID, and metadata. */
    public void emit(String actorId, String tenantId, AuditEventType eventType, String entityId, Map<String, String> metadata) {
        auditEvents.emit(
                eventType,
                new AuditSubject(actorId, tenantId, "RETRIEVAL", entityId),
                AuditMetadata.from(metadata));
    }
}
