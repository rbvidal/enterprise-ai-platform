package com.cognitera.platform.workspace.api;

import com.cognitera.platform.workspace.model.WorkspacePhase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** JPA entity recording a completed step within a workspace phase. */
@Entity
@Table(name = "workspace_steps")
public class WorkspaceStepEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private WorkspacePhase phase;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    public WorkspaceStepEntity() {}

    public WorkspaceStepEntity(String id, String workspaceId, WorkspacePhase phase, String stepName, String status) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        this.phase = phase;
        this.stepName = stepName;
        this.status = status;
        this.completedAt = Instant.now();
        this.data = "{}";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public WorkspacePhase getPhase() { return phase; }
    public void setPhase(WorkspacePhase phase) { this.phase = phase; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
