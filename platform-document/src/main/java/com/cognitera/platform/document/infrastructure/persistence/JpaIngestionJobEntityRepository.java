package com.cognitera.platform.document.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cognitera.platform.document.model.IngestionStatus;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link IngestionJobEntity} with status-based polling and sequence number queries. */
public interface JpaIngestionJobEntityRepository extends JpaRepository<IngestionJobEntity, UUID>, JpaSpecificationExecutor<IngestionJobEntity> {

    /** Finds the 10 oldest jobs with the given status. */
    List<IngestionJobEntity> findTop10ByStatusOrderByCreatedAtAsc(IngestionStatus status);

    /** Returns the maximum sequence number across all ingestion jobs, or 0 if none exist. */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(j.sequenceNumber), 0) FROM IngestionJobEntity j")
    long findMaxSequenceNumber();
}
