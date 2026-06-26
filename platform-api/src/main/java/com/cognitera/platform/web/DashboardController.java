package com.cognitera.platform.web;

import com.cognitera.platform.audit.api.AuditQuery;
import com.cognitera.platform.audit.api.AuditService;
import com.cognitera.platform.document.infrastructure.persistence.JpaDocumentEntityRepository;
import com.cognitera.platform.document.infrastructure.persistence.JpaIngestionJobEntityRepository;
import com.cognitera.platform.document.model.DocumentStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Renders the main dashboard with document counts, ingestion job status, and recent audit events. */
@Controller
public class DashboardController {

    private final JpaDocumentEntityRepository documentRepository;
    private final JpaIngestionJobEntityRepository ingestionJobRepository;
    private final AuditService auditService;

    public DashboardController(JpaDocumentEntityRepository documentRepository,
                               JpaIngestionJobEntityRepository ingestionJobRepository,
                               AuditService auditService) {
        this.documentRepository = documentRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.auditService = auditService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("documentCount", documentRepository.count());
        model.addAttribute("readyDocumentCount",
                documentRepository.count(
                        (root, query, cb) -> cb.equal(root.get("status"), DocumentStatus.READY)));
        model.addAttribute("ingestionJobCount", ingestionJobRepository.count());

        var auditPage = auditService.query(new AuditQuery(null, null, null, null, null,
                null, null, null, null, null, 0, 10));
        model.addAttribute("recentAudits", auditPage.events());

        return "dashboard";
    }
}
