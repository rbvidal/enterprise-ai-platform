package com.cognitera.platform.audit.infrastructure;

import com.cognitera.platform.audit.api.AuditEvent;
import com.cognitera.platform.audit.api.AuditEventPage;
import com.cognitera.platform.audit.api.AuditEventRepository;
import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditQuery;
import com.cognitera.platform.audit.api.AuditRequestContext;
import com.cognitera.platform.audit.api.AuditService;
import com.cognitera.platform.audit.api.AuditSource;
import com.cognitera.platform.audit.api.AuditSubject;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Default implementation of {@link AuditService} that persists events through an {@link AuditEventRepository}. */
@Service
public class PersistentAuditService implements AuditService {

    private final AuditEventRepository repository;

    public PersistentAuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void emit(AuditEvent event) {
        repository.append(event);
    }

    @Override
    public void emit(AuditEventType eventType, AuditSubject subject, AuditSource source) {
        AuditRequestContext context = AuditRequestContext.current();
        repository.append(new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                subject.actorId(),
                subject.tenantId(),
                eventType,
                subject.entityType(),
                subject.entityId(),
                source.module(),
                context.correlationId(),
                context.requestId(),
                context.requestPath(),
                context.httpMethod(),
                source.metadata()));
    }

    @Override
    public AuditEventPage query(AuditQuery query) {
        return repository.find(query);
    }
}
