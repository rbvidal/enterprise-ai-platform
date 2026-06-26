package com.cognitera.platform.workspace.api;

import com.cognitera.platform.workspace.model.DocumentType;

import java.time.Instant;
import java.util.Map;

/** DTO representing a document linked to a workspace. */
public record WorkspaceDocumentDto(
        String id,
        String workspaceId,
        String documentId,
        String documentName,
        DocumentType documentType,
        String documentCategory,
        Map<String, Object> extractedMetadata,
        Instant uploadedAt
) {}
