package com.cognitera.platform.workspace.infrastructure.persistence;

import com.cognitera.platform.workspace.api.TimelineEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data repository for {@link TimelineEventEntity} with workspace-based lookup and deletion. */
@Repository
public interface JpaTimelineEventRepository extends JpaRepository<TimelineEventEntity, String> {
    /** Finds timeline events for a workspace ordered by event date. */
    List<TimelineEventEntity> findByWorkspaceIdOrderByEventDate(String workspaceId);
    /** Deletes all timeline events belonging to a workspace. */
    void deleteByWorkspaceId(String workspaceId);
}
