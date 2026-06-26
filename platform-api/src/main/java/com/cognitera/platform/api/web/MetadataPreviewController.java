package com.cognitera.platform.api.web;

import com.cognitera.platform.document.api.TextExtractionService;
import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.document.model.DocumentVersion;
import com.cognitera.platform.search.api.MetadataExtractionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for AI-driven metadata extraction preview during file upload.
 */
@RestController
@RequestMapping("/api/ingestion")
public class MetadataPreviewController {

    private final ObjectProvider<MetadataExtractionService> extractionService;
    private final ObjectProvider<TextExtractionService> textExtractionService;

    /**
     * Constructs the controller with optional metadata extraction and text extraction services.
     */
    public MetadataPreviewController(
            ObjectProvider<MetadataExtractionService> extractionService,
            ObjectProvider<TextExtractionService> textExtractionService) {
        this.extractionService = extractionService;
        this.textExtractionService = textExtractionService;
    }

    /**
     * Extracts metadata suggestions from an uploaded file using the configured AI service.
     */
    @PostMapping("/preview-metadata")
    public ResponseEntity<Map<String, Object>> previewMetadata(@RequestParam("file") MultipartFile file) {
        MetadataExtractionService extractor = extractionService.getIfAvailable();
        if (extractor == null) {
            return ResponseEntity.unprocessableEntity().body(
                    Map.of("error", "AI metadata extraction not configured"));
        }
        try {
            Path tempFile = Files.createTempFile("platform_preview_", "_"
                    + sanitizeFileName(file.getOriginalFilename()));
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            String text = extractTextFromFile(tempFile, file.getOriginalFilename(),
                    file.getContentType());
            Files.deleteIfExists(tempFile);

            if (text == null || text.isBlank()) {
                Map<String, Object> emptySuggestion = new LinkedHashMap<>();
                emptySuggestion.put("suggestedTitle", null);
                emptySuggestion.put("confidence", 0.0);
                Map<String, Object> emptyResult = new LinkedHashMap<>();
                emptyResult.put("suggestion", emptySuggestion);
                emptyResult.put("extracted", false);
                return ResponseEntity.ok(emptyResult);
            }

            MetadataExtractionService.MetadataSuggestion suggestion = extractor.extractMetadata(
                    text, file.getOriginalFilename() != null
                            ? file.getOriginalFilename() : "document");

            Map<String, Object> suggestionMap = new LinkedHashMap<>();
            suggestionMap.put("suggestedTitle",
                    suggestion.suggestedTitle() != null ? suggestion.suggestedTitle() : "");
            suggestionMap.put("documentType",
                    suggestion.documentType() != null ? suggestion.documentType() : "");
            suggestionMap.put("category",
                    suggestion.category() != null ? suggestion.category() : "");
            suggestionMap.put("tags", suggestion.tags());
            suggestionMap.put("domain",
                    suggestion.domain() != null ? suggestion.domain() : "");
            suggestionMap.put("confidence", suggestion.confidence());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("suggestion", suggestionMap);
            result.put("extracted", suggestion.confidence() > 0.0);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorSuggestion = new LinkedHashMap<>();
            errorSuggestion.put("suggestedTitle", null);
            errorSuggestion.put("confidence", 0.0);
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("suggestion", errorSuggestion);
            errorResult.put("extracted", false);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    private String extractTextFromFile(Path path, String fileName, String contentType) throws IOException {
        if (fileName == null) {
            fileName = "";
        }
        String lower = fileName.toLowerCase();
        if (contentType != null && contentType.startsWith("text/") || lower.endsWith(".txt")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        TextExtractionService extractor = textExtractionService.getIfAvailable();
        if (extractor != null) {
            DocumentType docType = guessType(lower);
            DocumentVersion tempVersion = new DocumentVersion(
                    UUID.randomUUID(), 1, fileName,
                    contentType != null ? contentType : "application/octet-stream",
                    Files.size(path), "local-fs", path.toString().replace('\\', '/'),
                    null, "system", Instant.now());
            return extractor.extractText(docType, tempVersion);
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private DocumentType guessType(String lowerFileName) {
        if (lowerFileName.endsWith(".pdf")) return DocumentType.PDF;
        if (lowerFileName.endsWith(".docx")) return DocumentType.DOCX;
        if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) return DocumentType.HTML;
        return DocumentType.TXT;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "document";
        String sanitized = fileName.replace('\\', '/');
        int lastSlash = sanitized.lastIndexOf('/');
        return lastSlash >= 0 ? sanitized.substring(lastSlash + 1) : sanitized;
    }
}
