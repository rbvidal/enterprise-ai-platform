package com.cognitera.platform.api.web;

import com.cognitera.platform.api.dto.document.FailIngestionRequest;
import com.cognitera.platform.api.dto.document.IngestionJobPageResponse;
import com.cognitera.platform.api.dto.document.IngestionJobResponse;
import com.cognitera.platform.document.api.DocumentIngestionService;
import com.cognitera.platform.document.api.IngestionJobFilter;
import com.cognitera.platform.document.model.IngestionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing document ingestion job lifecycle.
 */
@RestController
@RequestMapping("/api/document-ingestion-jobs")
public class DocumentIngestionController {

    private final DocumentIngestionService ingestionService;

    /**
     * Constructs the controller with the ingestion service.
     */
    public DocumentIngestionController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Creates a new ingestion job for the specified document.
     */
    @PostMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public IngestionJobResponse create(@PathVariable UUID documentId, Authentication authentication) {
        return IngestionJobResponse.from(
                ingestionService.createIngestionJob(documentId, authentication.getName()));
    }

    /**
     * Returns a paginated list of ingestion jobs filtered by optional criteria.
     */
    @GetMapping
    public IngestionJobPageResponse find(
            @RequestParam(required = false) UUID documentId,
            @RequestParam(required = false) IngestionStatus status,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return IngestionJobPageResponse.from(ingestionService.findIngestionJobs(new IngestionJobFilter(
                documentId,
                status,
                tenantId,
                page,
                size)));
    }

    /**
     * Starts the processing of an ingestion job.
     */
    @PostMapping("/{jobId}/start")
    public IngestionJobResponse start(@PathVariable UUID jobId, Authentication authentication) {
        return IngestionJobResponse.from(
                ingestionService.startIngestion(jobId, authentication.getName()));
    }

    /**
     * Marks an ingestion job as completed.
     */
    @PostMapping("/{jobId}/complete")
    public IngestionJobResponse complete(@PathVariable UUID jobId, Authentication authentication) {
        return IngestionJobResponse.from(
                ingestionService.completeIngestion(jobId, authentication.getName()));
    }

    /**
     * Marks an ingestion job as failed with a reason.
     */
    @PostMapping("/{jobId}/fail")
    public IngestionJobResponse fail(
            @PathVariable UUID jobId,
            @Valid @RequestBody FailIngestionRequest request,
            Authentication authentication
    ) {
        return IngestionJobResponse.from(
                ingestionService.failIngestion(jobId, authentication.getName(), request.reason()));
    }
}
