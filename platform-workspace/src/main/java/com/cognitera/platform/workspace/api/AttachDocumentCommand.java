package com.cognitera.platform.workspace.api;

import com.cognitera.platform.workspace.model.DocumentType;

/** Command to attach a document to a workspace. */
public record AttachDocumentCommand(
        String workspaceId,
        String documentId,
        DocumentType documentType,
        String documentCategory,
        String notes
) {}
