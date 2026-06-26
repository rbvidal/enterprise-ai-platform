package com.cognitera.platform.web;

import com.cognitera.platform.document.api.DocumentIngestionService;
import com.cognitera.platform.document.api.IngestionJobFilter;
import com.cognitera.platform.document.model.IngestionStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Thymeleaf page controller for the ingestion jobs listing page.
 */
@Controller
public class JobsPageController {

    private final DocumentIngestionService ingestionService;

    /**
     * Constructs the controller with the ingestion service.
     */
    public JobsPageController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Renders the jobs list page, optionally filtered by ingestion status.
     */
    @GetMapping("/jobs")
    public String jobs(@RequestParam(required = false) IngestionStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("statuses", IngestionStatus.values());
        model.addAttribute("currentStatus", status);
        try {
            model.addAttribute("jobs", ingestionService.findIngestionJobs(
                    new IngestionJobFilter(null, status, null, page, 50)));
        } catch (Exception e) {
            model.addAttribute("jobs", new com.cognitera.platform.document.api.IngestionJobPage(
                    java.util.List.of(), 0, 50, 0, 0));
        }
        model.addAttribute("currentPage", page);
        return "jobs/list";
    }
}
