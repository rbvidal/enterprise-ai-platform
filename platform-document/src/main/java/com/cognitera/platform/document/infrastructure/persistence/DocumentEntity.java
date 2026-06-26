package com.cognitera.platform.document.infrastructure.persistence;

import com.cognitera.platform.document.model.DocumentStatus;
import com.cognitera.platform.document.model.DocumentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** JPA entity representing a document with metadata, status, tags, and version history. */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    private String category;

    @Column(name = "visibility", nullable = false)
    private String visibility;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("versionNumber ASC")
    private List<DocumentVersionEntity> versions = new ArrayList<>();

    protected DocumentEntity() {
    }

    public DocumentEntity(
            String tenantId,
            String title,
            DocumentType type,
            String category,
            Set<String> tags,
            String visibility,
            String actorId
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.title = title;
        this.type = type;
        this.status = DocumentStatus.INGESTION_PENDING;
        this.category = category;
        this.visibility = visibility;
        this.tags = new HashSet<>(tags);
        this.currentVersion = 0;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateMetadata(String title, DocumentType type, String category, Set<String> tags, String visibility, String actorId) {
        this.title = title;
        this.type = type;
        this.category = category;
        this.tags = new HashSet<>(tags);
        this.visibility = visibility;
        this.updatedBy = actorId;
    }

    public void addVersion(DocumentVersionEntity version, String actorId) {
        version.attachTo(this);
        this.versions.add(version);
        this.currentVersion = version.getVersionNumber();
        this.updatedBy = actorId;
    }

    public void markStatus(DocumentStatus status, String actorId) {
        this.status = status;
        this.updatedBy = actorId;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTitle() {
        return title;
    }

    public DocumentType getType() {
        return type;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getCategory() {
        return category;
    }

    public String getVisibility() {
        return visibility;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public List<DocumentVersionEntity> getVersions() {
        return List.copyOf(versions);
    }
}
