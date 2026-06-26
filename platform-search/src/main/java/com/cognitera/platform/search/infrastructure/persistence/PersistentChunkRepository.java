package com.cognitera.platform.search.infrastructure.persistence;

import com.cognitera.platform.search.api.ChunkRepository;
import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.SearchFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA-based implementation of {@link ChunkRepository} using specification-driven queries. */
@Repository
public class PersistentChunkRepository implements ChunkRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final JpaDocumentChunkRepository chunks;

    public PersistentChunkRepository(JpaDocumentChunkRepository chunks) {
        this.chunks = chunks;
    }

    @Override
    public DocumentChunk save(DocumentChunk chunk) {
        return SearchMapper.toModel(chunks.saveAndFlush(SearchMapper.toEntity(chunk)));
    }

    @Override
    public Optional<DocumentChunk> findById(UUID chunkId) {
        return chunks.findById(chunkId).map(SearchMapper::toModel);
    }

    @Override
    public List<DocumentChunk> find(SearchFilter filter, int page, int size) {
        return chunks.findAll(
                        DocumentChunkSpecifications.from(filter),
                        PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by(Sort.Direction.ASC, "chunkIndex")))
                .getContent()
                .stream()
                .map(SearchMapper::toModel)
                .toList();
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return 50;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
