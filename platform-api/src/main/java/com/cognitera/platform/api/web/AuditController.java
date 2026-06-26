package com.cognitera.platform.api.web;

import com.cognitera.platform.api.dto.audit.AuditEventPageResponse;
import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditQuery;
import com.cognitera.platform.audit.api.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * REST controller for querying audit events.
 */
@RestController
@RequestMapping("/api/audit/events")
public class AuditController {

    private final AuditService auditService;

    /**
     * Constructs the controller with the audit service.
     */
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Returns a paginated list of audit events, filtered by optional criteria.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AuditEventPageResponse findEvents(
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String sourceModule,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return AuditEventPageResponse.from(auditService.query(new AuditQuery(
                eventType,
                actorId,
                tenantId,
                entityType,
                entityId,
                sourceModule,
                correlationId,
                requestId,
                from,
                to,
                page,
                size)));
    }
}
