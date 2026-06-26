package com.cognitera.platform.web;

import com.cognitera.platform.audit.api.AuditEventPage;
import com.cognitera.platform.audit.api.AuditEventType;
import com.cognitera.platform.audit.api.AuditQuery;
import com.cognitera.platform.audit.api.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Thymeleaf page controller for the audit event listing page.
 */
@Controller
public class AuditPageController {

    private final AuditService auditService;

    /**
     * Constructs the controller with the audit service.
     */
    public AuditPageController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Renders the audit event page with optional filtering by event type, actor, or correlation ID.
     */
    @GetMapping("/audit")
    public String index(
            @RequestParam(required = false) AuditEventType eventType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        AuditQuery query = new AuditQuery(eventType, actorId, null, null, null,
                null, correlationId, null, null, null, page, 25);
        AuditEventPage result = auditService.query(query);

        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("form", new AuditForm(
                eventType != null ? eventType.name() : "",
                actorId != null ? actorId : "",
                correlationId != null ? correlationId : ""));
        model.addAttribute("page", result);
        model.addAttribute("currentPage", page);
        return "audit/index";
    }

    /**
     * Form-backing record for audit event filter parameters.
     */
    public record AuditForm(String eventType, String actorId, String correlationId) {}
}
