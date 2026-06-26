package com.cognitera.platform.workspace.infrastructure.persistence;

import com.cognitera.platform.workspace.api.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data repository for {@link WorkspaceEntity} with owner-based lookup. */
@Repository
public interface JpaWorkspaceRepository extends JpaRepository<WorkspaceEntity, String> {
    /** Finds workspaces belonging to a given owner. */
    List<WorkspaceEntity> findByOwnerId(String ownerId);
}
