package com.cognitera.platform.web;

import com.cognitera.platform.document.api.CreateDocumentCommand;
import com.cognitera.platform.document.api.DocumentFacade;
import com.cognitera.platform.document.api.DocumentFilter;
import com.cognitera.platform.document.model.DocumentStatus;
import com.cognitera.platform.document.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Thymeleaf page controller for document listing, upload, and viewing.
 */
@Controller
public class DocumentPageController {

    private static final Logger log = LoggerFactory.getLogger(DocumentPageController.class);

    private final DocumentFacade documentFacade;

    public DocumentPageController(DocumentFacade documentFacade) {
        this.documentFacade = documentFacade;
    }

    @GetMapping("/documents")
    public String list(Model model) {
        model.addAttribute("statuses", DocumentStatus.values());
        model.addAttribute("types", DocumentType.values());
        model.addAttribute("currentStatus", null);
        model.addAttribute("currentType", null);
        model.addAttribute("currentTenant", null);
        model.addAttribute("page", documentFacade.findDocuments(new DocumentFilter(
                null, null, null, null, null, null, null, 0, 50)));
        return "documents/list";
    }

    @GetMapping("/documents/upload")
    public String upload(Model model) {
        model.addAttribute("form", new UploadForm(null, "DOCUMENT", "PDF", null, null, null));
        model.addAttribute("types", types());
        return "documents/upload";
    }

    @PostMapping("/documents/upload")
    public String handleUpload(@ModelAttribute("form") UploadForm form,
                                @RequestParam("documentFile") MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No file selected");
            return "redirect:/documents/upload";
        }

        try {
            byte[] content = file.getBytes();
            String checksum = sha256(content);
            String contentType = detectContentType(file.getOriginalFilename());
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";

            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(UUID.randomUUID() + "_" + fileName);
            Files.write(filePath, content);

            DocumentType docType;
            try {
                docType = DocumentType.valueOf(form.type() != null ? form.type() : "PDF");
            } catch (IllegalArgumentException e) {
                docType = DocumentType.PDF;
            }
            Set<String> tagSet = form.tags() != null && !form.tags().isBlank()
                    ? Set.of(form.tags().split("\\s*,\\s*"))
                    : Set.of();

            var doc = documentFacade.createDocument(new CreateDocumentCommand(
                    form.title() != null && !form.title().isBlank() ? form.title() : fileName,
                    docType,
                    fileName,
                    contentType,
                    content.length,
                    "local-fs",
                    filePath.toString(),
                    checksum,
                    form.category() != null ? form.category() : "DOCUMENT",
                    tagSet,
                    "PRIVATE",
                    "system",
                    null));

            documentFacade.createIngestionJob(doc.id(), "system");

            redirectAttributes.addFlashAttribute("message", "Document uploaded: " + doc.metadata().title());
            return "redirect:/documents";
        } catch (IOException e) {
            log.error("Upload failed", e);
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/documents/upload";
        }
    }

    /**
     * Batch-uploads files with an optional tag. Each file is ingested using its
     * file name as the document title. Returns JSON with upload counts and errors.
     */
    @PostMapping(value = "/documents/batch", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleBatchUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "") String tag) {
        List<Map<String, String>> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> tagSet = tag.isBlank() ? Set.of() : Set.of(tag.trim().toLowerCase());

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                byte[] content = file.getBytes();
                String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
                String title = fileName.replaceAll("\\.[^.]+$", "");
                Path filePath = Path.of("uploads").resolve(UUID.randomUUID() + "_" + fileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, content);

                DocumentType docType = inferDocumentType(fileName);
                var doc = documentFacade.createDocument(new CreateDocumentCommand(
                        title,
                        docType,
                        fileName,
                        detectContentType(fileName),
                        content.length,
                        "local-fs",
                        filePath.toString(),
                        sha256(content),
                        "DOCUMENT",
                        tagSet,
                        "PRIVATE",
                        "system",
                        null));
                documentFacade.createIngestionJob(doc.id(), "system");

                uploaded.add(Map.of("id", doc.id().toString(), "title", title));
            } catch (Exception e) {
                log.error("Batch upload failed for {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploaded", uploaded.size());
        result.put("errors", errors.size());
        result.put("files", uploaded);
        result.put("errorDetails", errors);
        return ResponseEntity.ok(result);
    }

    private static DocumentType inferDocumentType(String fileName) {
        if (fileName == null) return DocumentType.PDF;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".docx")) return DocumentType.DOCX;
        if (lower.endsWith(".txt")) return DocumentType.TXT;
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return DocumentType.HTML;
        return DocumentType.PDF;
    }

    @GetMapping("/documents/{documentId}")
    public String view(@PathVariable String documentId,
                       @RequestParam(required = false) String chunk,
                       Model model) {
        try {
            var doc = documentFacade.getDocument(java.util.UUID.fromString(documentId), "system");
            model.addAttribute("doc", doc);
            model.addAttribute("highlightChunkId", chunk != null && !chunk.isBlank() ? chunk : "");
        } catch (Exception e) {
            model.addAttribute("doc", null);
            model.addAttribute("highlightChunkId", "");
            model.addAttribute("error", "Document not found: " + documentId);
        }
        return "documents/view";
    }

    private List<String> types() {
        return Arrays.stream(DocumentType.values()).map(Enum::name).toList();
    }

    private static String detectContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".md")) return "text/markdown";
        return "application/octet-stream";
    }

    private static String sha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(Arrays.hashCode(data));
        }
    }

    public record UploadForm(
            String title, String category, String type, String tags,
            String domain, String documentDate
    ) {}
}
