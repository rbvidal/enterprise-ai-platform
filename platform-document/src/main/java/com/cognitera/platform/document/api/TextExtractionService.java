package com.cognitera.platform.document.api;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.document.model.DocumentVersion;

/** Service for extracting raw text from document versions. */
public interface TextExtractionService {
    /** Extracts the full text from a document version given its type. */
    String extractText(DocumentType type, DocumentVersion version);
}
