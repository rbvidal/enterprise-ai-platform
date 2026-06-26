package com.cognitera.platform.neo4j.service;

import com.cognitera.platform.neo4j.config.Neo4jProperties;
import com.cognitera.platform.neo4j.model.EnrichmentResult;
import com.cognitera.platform.neo4j.model.GraphNode;
import com.cognitera.platform.neo4j.model.GraphRelationship;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Persists semantic enrichment results to Neo4j.
 * Auto-generates graph from documents — no manual CRUD.
 * Degrades gracefully when Neo4j is unavailable.
 */
@Service
@ConditionalOnBean(Driver.class)
public class GraphEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(GraphEnrichmentService.class);

    private final ObjectProvider<Driver> driverProvider;

    public GraphEnrichmentService(ObjectProvider<Driver> driverProvider) {
        this.driverProvider = driverProvider;
    }

    /** Returns true if Neo4j is connected. */
    public boolean isAvailable() {
        try {
            Driver driver = driverProvider.getIfAvailable();
            if (driver == null) return false;
            driver.verifyConnectivity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Persists an enrichment result to the graph. */
    public void persist(EnrichmentResult result) {
        Driver driver = driverProvider.getIfAvailable();
        if (driver == null) {
            log.debug("Neo4j not available — skipping enrichment persist for document {}", result.getDocumentId());
            return;
        }
        try (Session session = driver.session()) {
            for (GraphNode node : result.getNodes()) {
                upsertNode(session, node);
            }
            for (GraphRelationship rel : result.getRelationships()) {
                createRelationship(session, rel);
            }
            log.debug("Persisted {} nodes and {} relationships for document {}",
                    result.getNodes().size(), result.getRelationships().size(), result.getDocumentId());
        } catch (Exception e) {
            log.warn("Failed to persist enrichment to Neo4j for document {}: {}",
                    result.getDocumentId(), e.getMessage());
        }
    }

    /** Traverses the graph from a set of seed node IDs, returning related nodes up to maxDepth hops. */
    public List<GraphNode> traverse(List<String> seedIds, int maxDepth) {
        Driver driver = driverProvider.getIfAvailable();
        if (driver == null || seedIds.isEmpty()) return List.of();
        try (Session session = driver.session()) {
            var result = session.run(
                    "MATCH (n) WHERE n.id IN $seedIds " +
                    "MATCH (n)-[*1.." + maxDepth + "]-(related) " +
                    "RETURN DISTINCT related.id AS id, labels(related) AS labels, properties(related) AS props",
                    Map.of("seedIds", seedIds));
            return result.stream()
                    .map(r -> new GraphNode(
                            r.get("id").asString(),
                            parseNodeType(r.get("labels").asList()),
                            r.get("props").get("label", "?").toString(),
                            r.get("props").asMap()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Graph traversal failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** Returns all document IDs related to the given document through chunk/concept/entity links. */
    public List<String> findRelatedDocuments(String documentId, int maxDepth) {
        Driver driver = driverProvider.getIfAvailable();
        if (driver == null) return List.of();
        try (Session session = driver.session()) {
            var result = session.run(
                    "MATCH (d:Document {id: $docId})-[*1.." + maxDepth + "]-(other:Document) " +
                    "RETURN DISTINCT other.id AS id",
                    Map.of("docId", documentId));
            return result.stream().map(r -> r.get("id").asString()).toList();
        } catch (Exception e) {
            log.debug("Related documents query failed: {}", e.getMessage());
            return List.of();
        }
    }

    private void upsertNode(Session session, GraphNode node) {
        session.run("MERGE (n:" + node.type().name() + " {id: $id}) " +
                "SET n += $props",
                Values.parameters("id", node.id(), "props", node.properties()));
    }

    private void createRelationship(Session session, GraphRelationship rel) {
        session.run("MATCH (a {id: $sourceId}), (b {id: $targetId}) " +
                "MERGE (a)-[r:" + rel.type().name() + "]->(b) " +
                "SET r += $props",
                Values.parameters(
                        "sourceId", rel.sourceId(),
                        "targetId", rel.targetId(),
                        "props", rel.properties() != null ? rel.properties() : Map.of()));
    }

    private GraphNode.NodeType parseNodeType(List<Object> labels) {
        if (labels == null || labels.isEmpty()) return GraphNode.NodeType.CONCEPT;
        String label = labels.get(0).toString().toUpperCase();
        try { return GraphNode.NodeType.valueOf(label); }
        catch (IllegalArgumentException e) { return GraphNode.NodeType.CONCEPT; }
    }
}
