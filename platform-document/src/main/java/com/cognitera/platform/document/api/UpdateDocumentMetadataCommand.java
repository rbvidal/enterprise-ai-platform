package com.cognitera.platform.document.api;

import com.cognitera.platform.document.model.DocumentType;

import java.util.Set;
import java.util.UUID;

/** Command to update an existing document's metadata (title, type, category, tags, visibility). */
public record UpdateDocumentMetadataCommand(
        UUID documentId,
        String title,
        DocumentType type,
        String category,
        Set<String> tags,
        String visibility,
        String actorId
) {
}
