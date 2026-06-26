package com.cognitera.platform.document.api;

import com.cognitera.platform.document.model.DocumentType;

import java.util.Set;

/** Command to create a new document with file, metadata, and tenant information. */
public record CreateDocumentCommand(
        String title,
        DocumentType type,
        String fileName,
        String contentType,
        long sizeBytes,
        String storageProvider,
        String storageKey,
        String checksumSha256,
        String category,
        Set<String> tags,
        String visibility,
        String actorId,
        String tenantId
) {
}
