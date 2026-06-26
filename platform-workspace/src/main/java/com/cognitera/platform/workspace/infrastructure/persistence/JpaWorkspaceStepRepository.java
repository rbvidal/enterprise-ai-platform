package com.cognitera.platform.workspace.infrastructure.persistence;

import com.cognitera.platform.workspace.api.WorkspaceStepEntity;
import com.cognitera.platform.workspace.model.WorkspacePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link WorkspaceStepEntity} with workspace and phase-based lookups. */
@Repository
public interface JpaWorkspaceStepRepository extends JpaRepository<WorkspaceStepEntity, String> {
    /** Finds steps for a workspace ordered by completion time. */
    List<WorkspaceStepEntity> findByWorkspaceIdOrderByCompletedAt(String workspaceId);
    /** Finds a step by workspace ID and phase. */
    Optional<WorkspaceStepEntity> findByWorkspaceIdAndPhase(String workspaceId, WorkspacePhase phase);
    /** Deletes all steps belonging to a workspace. */
    void deleteByWorkspaceId(String workspaceId);
}
