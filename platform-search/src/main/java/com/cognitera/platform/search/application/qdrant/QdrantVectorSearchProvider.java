package com.cognitera.platform.search.application.qdrant;

import com.cognitera.platform.search.api.EmbeddingProvider;
import com.cognitera.platform.search.api.VectorSearchProvider;
import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.model.ChunkPosition;
import com.cognitera.platform.search.model.ChunkReference;
import com.cognitera.platform.search.model.CitationReference;
import com.cognitera.platform.search.model.DocumentChunk;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Qdrant-based {@link VectorSearchProvider} that indexes chunks with embeddings and performs vector similarity search. */
public class QdrantVectorSearchProvider implements VectorSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorSearchProvider.class);

    private final RestClient restClient;
    private final QdrantProperties properties;
    private final EmbeddingProvider embeddingProvider;

    public QdrantVectorSearchProvider(QdrantProperties properties, EmbeddingProvider embeddingProvider) {
        this.properties = properties;
        this.embeddingProvider = embeddingProvider;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    public List<RetrievalCandidate> search(SearchQuery query) {
        float[] queryVector = embeddingProvider.embed(query.query());
        int limit = Math.max(query.size(), 10);

        Map<String, Object> request = Map.of(
                "vector", queryVector,
                "limit", limit,
                "with_payload", true
        );

        QdrantSearchResponse response = restClient.post()
                .uri("/collections/{collection}/points/search", properties.collection())
                .body(request)
                .retrieve()
                .body(QdrantSearchResponse.class);

        if (response == null || response.result == null) {
            return List.of();
        }

        List<RetrievalCandidate> candidates = new ArrayList<>();
        for (QdrantScoredPoint point : response.result) {
            if (point.payload == null) {
                continue;
            }
            float score = point.score;
            String excerpt = point.payload.text();
            if (excerpt != null && excerpt.length() > 240) {
                excerpt = excerpt.substring(0, 240);
            }
            candidates.add(new RetrievalCandidate(
                    new ChunkReference(
                            UUID.fromString(point.payload.chunkId()),
                            UUID.fromString(point.payload.documentId()),
                            point.payload.documentVersion(),
                            point.payload.title(),
                            new ChunkPosition(null, null, point.payload.chunkIndex(), null, null),
                            parseDocType(point.payload.documentType())
                    ),
                    point.payload.text(),
                    0.0,
                    score,
                    score,
                    score,
                    "qdrant",
                    new CitationReference(
                            UUID.fromString(point.payload.documentId()),
                            UUID.fromString(point.payload.chunkId()),
                            point.payload.documentVersion(),
                            point.payload.title(),
                            null,
                            null,
                            null,
                            excerpt
                    )
            ));
        }
        return candidates;
    }

    @Override
    public void index(DocumentChunk chunk, float[] embedding) {
        Map<String, Object> point = Map.of(
                "id", chunk.id().toString(),
                "vector", embedding,
                "payload", toPayload(chunk)
        );

        Map<String, Object> request = Map.of("points", List.of(point));

        restClient.put()
                .uri("/collections/{collection}/points", properties.collection())
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.debug("Indexed chunk {} into Qdrant", chunk.id());
    }

    @Override
    public void indexBatch(List<DocumentChunk> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Chunks and embeddings must have same size");
        }
        if (chunks.isEmpty()) {
            return;
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            points.add(Map.of(
                    "id", chunks.get(i).id().toString(),
                    "vector", embeddings.get(i),
                    "payload", toPayload(chunks.get(i))
            ));
        }
        restClient.put()
                .uri("/collections/{collection}/points", properties.collection())
                .body(Map.of("points", points))
                .retrieve()
                .toBodilessEntity();

        log.debug("Indexed {} chunks into Qdrant", chunks.size());
    }

    @Override
    public void deleteByDocument(UUID documentId) {
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        Map.of("key", "documentId", "match", Map.of("value", documentId.toString()))
                )
        );
        restClient.post()
                .uri("/collections/{collection}/points/delete", properties.collection())
                .body(Map.of("filter", filter))
                .retrieve()
                .toBodilessEntity();

        log.debug("Deleted Qdrant points for document {}", documentId);
    }

    private QdrantPayload toPayload(DocumentChunk chunk) {
        return new QdrantPayload(
                chunk.id().toString(),
                chunk.documentId().toString(),
                chunk.documentVersion(),
                chunk.text(),
                chunk.metadata().title(),
                chunk.metadata().documentType() != null ? chunk.metadata().documentType().name() : null,
                chunk.metadata().category(),
                chunk.metadata().tags() != null ? String.join(",", chunk.metadata().tags()) : null,
                chunk.metadata().source(),
                chunk.metadata().tenantId(),
                chunk.metadata().documentCreatedAt() != null ? chunk.metadata().documentCreatedAt().toString() : null,
                chunk.position().chunkIndex()
        );
    }

    record QdrantPayload(
            String chunkId,
            String documentId,
            int documentVersion,
            String text,
            String title,
            String documentType,
            String category,
            String tags,
            String source,
            String tenantId,
            String documentCreatedAt,
            int chunkIndex
    ) {
    }

    record QdrantSearchResponse(List<QdrantScoredPoint> result) {
    }

    record QdrantScoredPoint(String id, float score, QdrantPayload payload) {
    }

    private static DocumentType parseDocType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return DocumentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
