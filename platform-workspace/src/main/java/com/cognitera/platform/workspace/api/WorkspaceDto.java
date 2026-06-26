package com.cognitera.platform.workspace.api;

import com.cognitera.platform.workspace.model.WorkspaceStatus;
import com.cognitera.platform.workspace.model.WorkspacePhase;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full DTO for a workspace including documents, timeline events, phase, and status. */
public record WorkspaceDto(
        String id,
        String name,
        String description,
        String workspaceType,
        WorkspaceStatus status,
        WorkspacePhase phase,
        String ownerId,
        Map<String, Object> phaseData,
        List<WorkspaceDocumentDto> documents,
        List<TimelineEventDto> timelineEvents,
        Instant createdAt,
        Instant updatedAt
) {}
