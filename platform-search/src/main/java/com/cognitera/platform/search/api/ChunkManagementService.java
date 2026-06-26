package com.cognitera.platform.search.api;

import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.SearchFilter;

import java.util.List;
import java.util.UUID;

/** Service for managing the lifecycle of document chunks. */
public interface ChunkManagementService {
    /** Indexes a single chunk from a command. */
    DocumentChunk indexChunk(IndexChunkCommand command);

    /** Retrieves a chunk by its ID. */
    DocumentChunk getChunk(UUID chunkId);

    /** Finds chunks matching the given filter with pagination. */
    List<DocumentChunk> findChunks(SearchFilter filter, int page, int size);
}
