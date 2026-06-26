package com.cognitera.platform.neo4j.model;

import java.util.Map;

/** A relationship between two nodes in the knowledge graph. */
public record GraphRelationship(
        String sourceId,
        String targetId,
        RelationshipType type,
        Map<String, Object> properties
) {
    public enum RelationshipType {
        REFERENCES, DEPENDS_ON, PART_OF, RELATED_TO,
        IMPLEMENTS, USES, MENTIONS, BELONGS_TO, DERIVED_FROM
    }
}
