package com.cognitera.platform.api.config;

import com.cognitera.platform.neo4j.model.GraphNode;
import com.cognitera.platform.neo4j.service.GraphEnrichmentService;
import com.cognitera.platform.search.api.GraphSearchProvider;
import com.cognitera.platform.search.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Adapter bridging Neo4j graph enrichment to the search pipeline's GraphSearchProvider SPI.
 * Enables GraphRAG: graph traversal results participate in retrieval fusion.
 */
@Component
@ConditionalOnProperty(name = "platform.neo4j.uri")
public class Neo4jGraphSearchAdapter implements GraphSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphSearchAdapter.class);

    private final GraphEnrichmentService graphService;

    public Neo4jGraphSearchAdapter(GraphEnrichmentService graphService) {
        this.graphService = graphService;
    }

    @Override
    public boolean isAvailable() {
        return graphService.isAvailable();
    }

    @Override
    public List<RetrievalCandidate> search(SearchQuery query) {
        if (!isAvailable() || query.query() == null || query.query().isBlank()) return List.of();
        try {
            List<String> seedIds = extractEntityIds(query.query());
            if (seedIds.isEmpty()) return List.of();

            List<GraphNode> relatedNodes = graphService.traverse(seedIds, 2);
            if (relatedNodes.isEmpty()) return List.of();

            Instant now = Instant.now();
            List<RetrievalCandidate> candidates = new ArrayList<>();
            for (int i = 0; i < relatedNodes.size(); i++) {
                GraphNode node = relatedNodes.get(i);
                String label = node.getLabel();
                String docId = node.getProvenance() != null
                        ? node.getProvenance().sourceDocumentId() : "graph";
                UUID chunkId = UUID.nameUUIDFromBytes((node.getId() + i).getBytes());
                UUID docUuid = safeUuid(docId);

                ChunkReference chunkRef = new ChunkReference(chunkId, docUuid, 1,
                        label, new ChunkPosition(null, null, i, null, null));

                CitationReference citation = new CitationReference(
                        docUuid, chunkId, 1, label,
                        null, null, null,
                        label.length() > 200 ? label.substring(0, 200) : label);

                double graphScore = 0.5 + (node.getProvenance() != null
                        ? node.getProvenance().extractionConfidence() * 0.3 : 0.0);

                candidates.add(new RetrievalCandidate(chunkRef, label,
                        0.0, 0.0, Math.min(1.0, graphScore),
                        0.3, "graph", citation));
            }
            log.debug("GraphRAG: {} candidates from {} seed IDs", candidates.size(), seedIds.size());
            return candidates;
        } catch (Exception e) {
            log.debug("Graph search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<String> findRelatedDocuments(String documentId, int maxDepth) {
        if (!isAvailable()) return List.of();
        return graphService.findRelatedDocuments(documentId, maxDepth);
    }

    private List<String> extractEntityIds(String query) {
        return Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3)
                .map(w -> "entity/" + w)
                .limit(10)
                .toList();
    }

    private static UUID safeUuid(String s) {
        try { return UUID.fromString(s); }
        catch (IllegalArgumentException e) { return UUID.nameUUIDFromBytes(s.getBytes()); }
    }
}
