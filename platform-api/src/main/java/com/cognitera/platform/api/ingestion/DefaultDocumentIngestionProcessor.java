package com.cognitera.platform.api.ingestion;

import com.cognitera.platform.document.api.DocumentFacade;
import com.cognitera.platform.document.api.DocumentIngestionProcessor;
import com.cognitera.platform.document.api.TextExtractionService;
import com.cognitera.platform.document.model.DocumentVersion;
import com.cognitera.platform.document.model.Document;
import com.cognitera.platform.search.api.ChunkManagementService;
import com.cognitera.platform.search.api.IndexChunkCommand;
import com.cognitera.platform.search.api.IndexingOrchestrationService;
import com.cognitera.platform.search.model.ChunkType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link DocumentIngestionProcessor} that extracts text
 * and indexes document chunks, falling back to keyword-only indexing when no
 * embedding infrastructure is available.
 */
@Component
public class DefaultDocumentIngestionProcessor implements DocumentIngestionProcessor {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 150;

    private final DocumentFacade documents;
    private final ChunkManagementService chunks;
    private final TextExtractionService textExtractionService;
    private final ObjectProvider<IndexingOrchestrationService> indexingOrchestrator;

    /**
     * Constructs the processor with required dependencies.
     */
    public DefaultDocumentIngestionProcessor(
            DocumentFacade documents,
            ChunkManagementService chunks,
            TextExtractionService textExtractionService,
            ObjectProvider<IndexingOrchestrationService> indexingOrchestrator) {
        this.documents = documents;
        this.chunks = chunks;
        this.textExtractionService = textExtractionService;
        this.indexingOrchestrator = indexingOrchestrator;
    }

    /**
     * Ingests a document by extracting its text, chunking it, and indexing each chunk.
     */
    @Override
    public void ingest(UUID documentId) {
        IndexingOrchestrationService orchestrator = indexingOrchestrator.getIfAvailable();
        if (orchestrator != null) {
            orchestrator.indexDocument(documentId);
            return;
        }
        // Fallback: keyword-only indexing when embedding infrastructure is not configured
        ingestKeywordOnly(documentId);
    }

    private void ingestKeywordOnly(UUID documentId) {
        Document document = documents.getDocument(documentId, "system");
        DocumentVersion version = document.versions().stream()
                .filter(v -> v.versionNumber() == document.currentVersion())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Document has no current version"));

        String extractedText = textExtractionService.extractText(document.metadata().type(), version);
        List<String> textChunks = chunkText(extractedText);
        for (int i = 0; i < textChunks.size(); i++) {
            chunks.indexChunk(new IndexChunkCommand(
                    document.id(),
                    version.versionNumber(),
                    ChunkType.TEXT,
                    textChunks.get(i),
                    null, null, i, null, null,
                    document.metadata().title(),
                    document.metadata().type(),
                    document.metadata().category(),
                    document.metadata().tags(),
                    "upload",
                    document.tenantId(),
                    document.createdAt(),
                    List.of(),
                    null
            ));
        }
    }

    private List<String> chunkText(String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            throw new IllegalStateException("No extractable text found");
        }
        List<String> chunks = new ArrayList<>();
        int index = 0;
        while (index < normalized.length()) {
            int end = Math.min(index + CHUNK_SIZE, normalized.length());
            String slice = normalized.substring(index, end).trim();
            if (!slice.isBlank()) {
                chunks.add(slice);
            }
            if (end == normalized.length()) {
                break;
            }
            index = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }
}
