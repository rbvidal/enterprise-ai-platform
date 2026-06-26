package com.cognitera.platform.ai.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Publishes AI operations to the platform audit log via structured logging. */
@Component
public class AiAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AiAuditPublisher.class);

    /** Emits an auditable event for an AI operation. */
    public void emit(String actorId, String tenantId, String eventType, String requestId, Map<String, String> metadata) {
        log.info("AI_AUDIT eventType={} actorId={} requestId={} metadata={}",
                eventType, actorId != null ? actorId : "system", requestId, metadata);
    }
}
