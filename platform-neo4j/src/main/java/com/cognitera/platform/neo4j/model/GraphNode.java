package com.cognitera.platform.neo4j.model;

import java.util.List;
import java.util.Map;

/** A node in the knowledge graph generated from document enrichment. */
public record GraphNode(
        String id,
        NodeType type,
        String label,
        Map<String, Object> properties
) {
    public enum NodeType {
        DOCUMENT, CHUNK, ENTITY, CONCEPT, TOPIC,
        ORGANIZATION, PERSON, TECHNOLOGY, REGULATION, PROJECT
    }
}
