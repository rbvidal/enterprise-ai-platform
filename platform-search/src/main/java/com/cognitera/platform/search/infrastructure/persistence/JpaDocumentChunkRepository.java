package com.cognitera.platform.search.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/** Spring Data repository for {@link DocumentChunkEntity} with specification-based query and bulk delete by document. */
public interface JpaDocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID>, JpaSpecificationExecutor<DocumentChunkEntity> {

    /** Deletes all chunks belonging to a document and returns the count of deleted rows. */
    @Modifying
    @Query("delete from DocumentChunkEntity c where c.documentId = :documentId")
    int deleteByDocumentId(UUID documentId);
}
