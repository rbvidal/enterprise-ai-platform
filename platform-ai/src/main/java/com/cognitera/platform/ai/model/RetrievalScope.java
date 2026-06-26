package com.cognitera.platform.ai.model;

/**
 * The scope of retrieval: current workspace, current tenant, all documents, authoritative only, or hybrid.
 */
public enum RetrievalScope {
    CURRENT_WORKSPACE,
    CURRENT_TENANT,
    ALL_DOCUMENTS,
    AUTHORITATIVE_ONLY,
    HYBRID
}
