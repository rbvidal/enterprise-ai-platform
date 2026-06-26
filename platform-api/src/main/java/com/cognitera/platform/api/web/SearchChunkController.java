package com.cognitera.platform.api.web;

import com.cognitera.platform.api.dto.search.DocumentChunkResponse;
import com.cognitera.platform.api.dto.search.IndexChunkRequest;
import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.api.ChunkManagementService;
import com.cognitera.platform.search.api.IndexChunkCommand;
import com.cognitera.platform.search.model.MetadataFilter;
import com.cognitera.platform.search.model.SearchFilter;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for managing indexed document chunks.
 */
@RestController
@RequestMapping("/api/search/chunks")
public class SearchChunkController {

    private final ChunkManagementService chunks;

    /**
     * Constructs the controller with the chunk management service.
     */
    public SearchChunkController(ChunkManagementService chunks) {
        this.chunks = chunks;
    }

    /**
     * Indexes a new document chunk for search.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentChunkResponse index(@Valid @RequestBody IndexChunkRequest request) {
        return DocumentChunkResponse.from(chunks.indexChunk(new IndexChunkCommand(
                request.documentId(),
                request.documentVersion(),
                request.chunkType(),
                request.text(),
                request.pageNumber(),
                request.sectionIndex(),
                request.chunkIndex(),
                request.startOffset(),
                request.endOffset(),
                request.title(),
                request.documentType(),
                request.category(),
                request.tags(),
                request.source(),
                request.tenantId(),
                request.documentCreatedAt(),
                attributes(request),
                request.embeddingReference())));
    }

    /**
     * Retrieves a specific chunk by its identifier.
     */
    @GetMapping("/{chunkId}")
    public DocumentChunkResponse get(@PathVariable UUID chunkId) {
        return DocumentChunkResponse.from(chunks.getChunk(chunkId));
    }

    /**
     * Returns a paginated list of chunks filtered by optional criteria.
     */
    @GetMapping
    public List<DocumentChunkResponse> find(
            @RequestParam(required = false) Set<UUID> documentIds,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return chunks.findChunks(new SearchFilter(
                        documentIds,
                        documentType,
                        category,
                        tag,
                        source,
                        tenantId,
                        createdFrom,
                        createdTo,
                        List.of()),
                page,
                size)
                .stream()
                .map(DocumentChunkResponse::from)
                .toList();
    }

    private List<MetadataFilter> attributes(IndexChunkRequest request) {
        if (request.attributes() == null) {
            return List.of();
        }
        return request.attributes().stream()
                .map(attribute -> new MetadataFilter(attribute.key(), attribute.value()))
                .toList();
    }
}
