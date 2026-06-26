package com.cognitera.platform.search.application;

import com.cognitera.platform.document.api.DocumentFacade;
import com.cognitera.platform.document.api.TextExtractionService;
import com.cognitera.platform.document.model.Document;
import com.cognitera.platform.document.model.DocumentVersion;
import com.cognitera.platform.search.api.ChunkManagementService;
import com.cognitera.platform.search.api.ChunkingStrategy;
import com.cognitera.platform.search.api.EmbeddingProvider;
import com.cognitera.platform.search.api.IndexChunkCommand;
import com.cognitera.platform.search.api.IndexingOrchestrationService;
import com.cognitera.platform.search.api.VectorSearchProvider;
import com.cognitera.platform.search.model.ChunkPosition;
import com.cognitera.platform.search.model.ChunkType;
import com.cognitera.platform.search.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Orchestrates document indexing by extracting text, chunking, generating embeddings, and storing in the vector DB. */
public class DefaultIndexingOrchestrationService implements IndexingOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIndexingOrchestrationService.class);

    private final DocumentFacade documents;
    private final TextExtractionService textExtractionService;
    private final ChunkingStrategy chunkingStrategy;
    private final ChunkManagementService chunkManagementService;
    private final EmbeddingProvider embeddingProvider;
    private final VectorSearchProvider vectorSearchProvider;

    public DefaultIndexingOrchestrationService(
            DocumentFacade documents,
            TextExtractionService textExtractionService,
            ChunkingStrategy chunkingStrategy,
            ChunkManagementService chunkManagementService,
            EmbeddingProvider embeddingProvider,
            VectorSearchProvider vectorSearchProvider
    ) {
        this.documents = documents;
        this.textExtractionService = textExtractionService;
        this.chunkingStrategy = chunkingStrategy;
        this.chunkManagementService = chunkManagementService;
        this.embeddingProvider = embeddingProvider;
        this.vectorSearchProvider = vectorSearchProvider;
    }

    @Override
    public void indexDocument(UUID documentId) {
        Document document = documents.getDocument(documentId, "system");
        DocumentVersion version = document.versions().stream()
                .filter(v -> v.versionNumber() == document.currentVersion())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Document has no current version"));

        String extractedText = textExtractionService.extractText(document.metadata().type(), version);
        List<DocumentChunk> rawChunks = chunkingStrategy.chunk(
                document.id(), version.versionNumber(), document.metadata().title(), extractedText);

        if (rawChunks.isEmpty()) {
            log.warn("No chunks produced for document {}", documentId);
            return;
        }

        List<String> chunkTexts = rawChunks.stream().map(DocumentChunk::text).toList();
        List<float[]> embeddings = embeddingProvider.embedBatch(chunkTexts);

        List<DocumentChunk> enrichedChunks = new ArrayList<>();
        for (int i = 0; i < rawChunks.size(); i++) {
            DocumentChunk raw = rawChunks.get(i);
            DocumentChunk enriched = persistChunk(document, version, raw, i);
            enrichedChunks.add(enriched);
        }

        vectorSearchProvider.indexBatch(enrichedChunks, embeddings);
        log.info("Indexed {} chunks for document {} ({}d vectors)", enrichedChunks.size(), documentId, embeddingProvider.dimension());
    }

    @Override
    public void reindexDocument(UUID documentId) {
        deleteDocumentChunks(documentId);
        indexDocument(documentId);
    }

    @Override
    public void deleteDocumentChunks(UUID documentId) {
        vectorSearchProvider.deleteByDocument(documentId);
    }

    private DocumentChunk persistChunk(Document document, DocumentVersion version, DocumentChunk raw, int chunkIndex) {
        ChunkPosition position = raw.position();
        return chunkManagementService.indexChunk(new IndexChunkCommand(
                document.id(),
                version.versionNumber(),
                raw.type() != null ? raw.type() : ChunkType.TEXT,
                raw.text(),
                position.pageNumber(),
                position.sectionIndex(),
                chunkIndex,
                position.startOffset(),
                position.endOffset(),
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
