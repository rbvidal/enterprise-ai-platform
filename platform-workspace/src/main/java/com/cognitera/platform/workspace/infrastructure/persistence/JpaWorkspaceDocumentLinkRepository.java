package com.cognitera.platform.workspace.infrastructure.persistence;

import com.cognitera.platform.workspace.api.WorkspaceDocumentLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data repository for {@link WorkspaceDocumentLinkEntity} with workspace-based lookup. */
@Repository
public interface JpaWorkspaceDocumentLinkRepository extends JpaRepository<WorkspaceDocumentLinkEntity, String> {
    /** Finds all document links for a given workspace. */
    List<WorkspaceDocumentLinkEntity> findByWorkspaceId(String workspaceId);
}
