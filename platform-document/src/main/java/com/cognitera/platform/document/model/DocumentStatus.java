package com.cognitera.platform.document.model;

/** Lifecycle status of a document (draft, pending, ingesting, ready, failed, archived, deleted). */
public enum DocumentStatus {
    DRAFT,
    INGESTION_PENDING,
    INGESTING,
    READY,
    FAILED,
    ARCHIVED,
    DELETED
}
