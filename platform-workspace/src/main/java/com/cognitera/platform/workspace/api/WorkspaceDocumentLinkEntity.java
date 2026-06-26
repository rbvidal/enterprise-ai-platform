package com.cognitera.platform.workspace.api;

import com.cognitera.platform.workspace.model.DocumentType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** JPA entity linking a document to a workspace. */
@Entity
@Table(name = "workspace_documents")
public class WorkspaceDocumentLinkEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "document_name")
    private String documentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "document_category")
    private String documentCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_metadata", columnDefinition = "jsonb")
    private String extractedMetadata;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public WorkspaceDocumentLinkEntity() {}

    public WorkspaceDocumentLinkEntity(String id, String workspaceId, String documentId, String documentName,
                                       DocumentType documentType, String documentCategory) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.workspaceId = workspaceId;
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.documentCategory = documentCategory != null ? documentCategory : "general";
        this.extractedMetadata = "{}";
        this.uploadedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getDocumentCategory() { return documentCategory; }
    public void setDocumentCategory(String documentCategory) { this.documentCategory = documentCategory; }
    public String getExtractedMetadata() { return extractedMetadata; }
    public void setExtractedMetadata(String extractedMetadata) { this.extractedMetadata = extractedMetadata; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
