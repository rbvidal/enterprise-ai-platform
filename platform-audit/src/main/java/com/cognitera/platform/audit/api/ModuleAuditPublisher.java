package com.cognitera.platform.audit.api;

import com.fasterxml.jackson.databind.JsonNode;

/** Base interface for module-specific audit publishers. */
public interface ModuleAuditPublisher {
    /** Emits an audit event with the given type, subject, and JSON metadata. */
    void emit(AuditEventType eventType, AuditSubject subject, JsonNode metadata);
}
