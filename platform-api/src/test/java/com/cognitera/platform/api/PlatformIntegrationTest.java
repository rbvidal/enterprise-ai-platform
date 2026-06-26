package com.cognitera.platform.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "platform.auth.jwt-secret=test-secret-that-is-at-least-32-bytes-long-for-hs256"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Platform Integration Tests")
class PlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"tester@test.com","password":"Pass1234!","displayName":"Test User","roles":[]}
                    """));

        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "tester@test.com")
                .param("password", "Pass1234!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        session = (MockHttpSession) result.getRequest().getSession();
    }

    @Nested
    @DisplayName("Navigation & Page Access")
    class Navigation {

        @Test
        @DisplayName("dashboard returns 200")
        void dashboardPage() throws Exception {
            mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("workspaces list returns 200")
        void workspacesPage() throws Exception {
            mockMvc.perform(get("/workspaces").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Workspaces")));
        }

        @Test
        @DisplayName("documents page returns 200")
        void documentsPage() throws Exception {
            mockMvc.perform(get("/documents").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Documents")));
        }

        @Test
        @DisplayName("upload page returns 200 with simplified form")
        void uploadPage() throws Exception {
            mockMvc.perform(get("/documents/upload").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Document Ingestion")));
        }

        @Test
        @DisplayName("jobs page returns 200")
        void jobsPage() throws Exception {
            mockMvc.perform(get("/jobs").session(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("search page returns 200 without tenantId field")
        void searchPage() throws Exception {
            mockMvc.perform(get("/search").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Document Search")))
                    .andExpect(content().string(not(containsString("tenantId"))));
        }

        @Test
        @DisplayName("chunks page returns 200")
        void chunksPage() throws Exception {
            mockMvc.perform(get("/chunks").session(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("audit page returns 200 without tenantId field")
        void auditPage() throws Exception {
            mockMvc.perform(get("/audit").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Audit Viewer")))
                    .andExpect(content().string(not(containsString("tenantId"))))
                    .andExpect(content().string(not(containsString("Tenant"))));
        }

        @Test
        @DisplayName("AI page returns 200 without legacy Evidence labels")
        void aiPage() throws Exception {
            mockMvc.perform(get("/ai").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Grounded AI Orchestration")))
                    .andExpect(content().string(not(containsString("PRIMARY EVIDENCE"))))
                    .andExpect(content().string(not(containsString("All evidence"))));
        }
    }

    @Nested
    @DisplayName("Workspace Wizard Navigation")
    class WizardNavigation {

        private String wsId;

        @BeforeEach
        void createWorkspace() throws Exception {
            var result = mockMvc.perform(post("/workspaces/new")
                    .session(session)
                    .param("workspaceType", "GENERAL")
                    .param("domain", "GENERAL")
                    .param("workspaceReference", "WS-WIZARD-TEST")
                    .param("additionalNotes", "Testing wizard navigation"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            String location = result.getResponse().getRedirectedUrl();
            wsId = location.replaceAll(".*/workspaces/([^/]+)/wizard.*", "$1");
        }

        @Test
        @DisplayName("wizard shows first stage SETUP")
        void wizardShowsSetupStage() throws Exception {
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Setup")))
                    .andExpect(content().string(containsString("Workspace has been created")));
        }

        @Test
        @DisplayName("advance moves through all 5 stages to COMPLETE")
        void advanceThroughAllStages() throws Exception {
            // Advance: SETUP → INGESTION
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ingestion")));

            // Advance: INGESTION → ANALYSIS
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Analysis")));

            // Advance: ANALYSIS → REVIEW
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Review")));

            // Advance: REVIEW → COMPLETE
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Complete")));

            // Advance from COMPLETE should stay at COMPLETE
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Complete")));
        }

        @Test
        @DisplayName("previous moves back through stages")
        void previousMovesBack() throws Exception {
            // Advance to INGESTION first
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session));
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session));

            // Should be at ANALYSIS now
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(content().string(containsString("Analysis")));

            // Go back to INGESTION
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/previous").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(content().string(containsString("Ingestion")));

            // Go back to SETUP
            mockMvc.perform(post("/workspaces/" + wsId + "/wizard/previous").session(session))
                    .andExpect(status().is3xxRedirection());
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(content().string(containsString("Setup")));
        }

        @Test
        @DisplayName("previous button disabled on first stage (SETUP)")
        void previousDisabledOnFirstStage() throws Exception {
            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("disabled")))
                    .andExpect(content().string(containsString("Previous")));
        }

        @Test
        @DisplayName("next button shows Complete on last stage")
        void nextButtonShowsCompleteOnLastStage() throws Exception {
            // Advance all the way to COMPLETE
            for (int i = 0; i < 4; i++) {
                mockMvc.perform(post("/workspaces/" + wsId + "/wizard/advance").session(session));
            }

            mockMvc.perform(get("/workspaces/" + wsId + "/wizard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Complete")));
        }
    }

    @Nested
    @DisplayName("Workspace Creation — Generalized Dropdowns")
    class GeneralizedDropdowns {

        @Test
        @DisplayName("creation form has no law-specific workspace types")
        void noLawSpecificWorkspaceTypes() throws Exception {
            mockMvc.perform(get("/workspaces/new").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("LEGAL_CASE"))))
                    .andExpect(content().string(containsString("GENERAL")))
                    .andExpect(content().string(containsString("PROJECT_MANAGEMENT")))
                    .andExpect(content().string(containsString("DATA_ANALYSIS")));
        }

        @Test
        @DisplayName("can create workspace with new enum values")
        void createWorkspaceWithNewValues() throws Exception {
            mockMvc.perform(post("/workspaces/new")
                    .session(session)
                    .param("workspaceType", "PROJECT_MANAGEMENT")
                    .param("domain", "ANALYSIS")
                    .param("workspaceReference", "WS-AN-001"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/workspaces/*/wizard"));
        }
    }

    @Nested
    @DisplayName("Search — No Court/Statute Types")
    class SearchNoLegalTypes {

        @Test
        @DisplayName("search form has no COURT_DECISION or STATUTE in type dropdown")
        void noCourtDecisionInTypes() throws Exception {
            mockMvc.perform(get("/search").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("COURT_DECISION"))))
                    .andExpect(content().string(not(containsString("STATUTE"))))
                    .andExpect(content().string(containsString("PDF")))
                    .andExpect(content().string(containsString("DOCX")));
        }

        @Test
        @DisplayName("search page has no tenantId input")
        void noTenantIdInSearch() throws Exception {
            mockMvc.perform(get("/search").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("name=\"tenantId\""))));
        }
    }

    @Nested
    @DisplayName("Documents — No Tenant ID Filter")
    class DocumentsNoTenant {

        @Test
        @DisplayName("documents list has no tenantId filter")
        void noTenantIdInDocumentsList() throws Exception {
            mockMvc.perform(get("/documents").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("name=\"tenantId\""))));
        }

        @Test
        @DisplayName("jobs list has no tenantId filter")
        void noTenantIdInJobsList() throws Exception {
            mockMvc.perform(get("/jobs").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("name=\"tenantId\""))));
        }
    }

    @Nested
    @DisplayName("Workspace Detail — Generalized Labels")
    class WorkspaceDetailGeneralized {

        private String wsId;

        @BeforeEach
        void createWorkspace() throws Exception {
            var result = mockMvc.perform(post("/workspaces/new")
                    .session(session)
                    .param("workspaceType", "RESEARCH")
                    .param("domain", "RESEARCH")
                    .param("workspaceReference", "WS-DETAIL-TEST"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            String location = result.getResponse().getRedirectedUrl();
            wsId = location.replaceAll(".*/workspaces/([^/]+)/wizard.*", "$1");
        }

        @Test
        @DisplayName("detail page uses 'Documents' not 'Evidence'")
        void usesDocumentsNotEvidence() throws Exception {
            mockMvc.perform(get("/workspaces/" + wsId + "/detail").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Documents")))
                    .andExpect(content().string(not(containsString("Evidence"))));
        }

        @Test
        @DisplayName("detail page shows documents section and no legacy legal labels")
        void usesDocumentsNotNormative() throws Exception {
            mockMvc.perform(get("/workspaces/" + wsId + "/detail").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Documents")))
                    .andExpect(content().string(not(containsString("Evidence"))))
                    .andExpect(content().string(not(containsString("Normative Authority"))));
        }
    }

    @Nested
    @DisplayName("Upload Form — Simplified")
    class UploadFormSimplified {

        @Test
        @DisplayName("upload form has no matter/tenant field")
        void noMatterTenantInUpload() throws Exception {
            mockMvc.perform(get("/documents/upload").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("tenantId"))))
                    .andExpect(content().string(not(containsString("Matter"))))
                    .andExpect(content().string(not(containsString("tenant"))));
        }

        @Test
        @DisplayName("upload form has no jurisdiction or authority fields")
        void noJurisdictionOrAuthority() throws Exception {
            mockMvc.perform(get("/documents/upload").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("jurisdiction"))))
                    .andExpect(content().string(not(containsString("Jurisdiction"))))
                    .andExpect(content().string(not(containsString("authority"))))
                    .andExpect(content().string(not(containsString("Authority"))));
        }

        @Test
        @DisplayName("upload form has no contract-specific fields")
        void noContractSpecificFields() throws Exception {
            mockMvc.perform(get("/documents/upload").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("contractType"))))
                    .andExpect(content().string(not(containsString("parties"))))
                    .andExpect(content().string(not(containsString("effectiveDate"))));
        }

        @Test
        @DisplayName("upload form has simplified metadata fields")
        void hasSimplifiedFields() throws Exception {
            mockMvc.perform(get("/documents/upload").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Document title")))
                    .andExpect(content().string(containsString("Document date")));
        }
    }

    @Nested
    @DisplayName("Dashboard — Recent Audits")
    class DashboardRecentAudits {

        @Test
        @DisplayName("dashboard shows metrics and loads without error")
        void dashboardLoads() throws Exception {
            mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Dashboard")))
                    .andExpect(content().string(containsString("Documents")))
                    .andExpect(content().string(containsString("Recent audit events")));
        }

        @Test
        @DisplayName("root path redirects to dashboard")
        void rootPathLoads() throws Exception {
            mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Document View — Detail Page")
    class DocumentViewDetail {

        @Test
        @DisplayName("document view page returns 200 for valid UUID format")
        void documentViewValidUuidFormat() throws Exception {
            mockMvc.perform(get("/documents/00000000-0000-0000-0000-000000000000").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Document Viewer")));
        }

        @Test
        @DisplayName("document view handles chunk parameter")
        void documentViewWithChunk() throws Exception {
            mockMvc.perform(get("/documents/00000000-0000-0000-0000-000000000000")
                    .param("chunk", "abc").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Document Viewer")));
        }
    }

    @Nested
    @DisplayName("Error Page")
    class ErrorPageHandling {

        @Test
        @DisplayName("error page renders with status code")
        void errorPageRenders() throws Exception {
            mockMvc.perform(get("/error").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Error")))
                    .andExpect(content().string(containsString("500")));
        }
    }

    @Nested
    @DisplayName("AI Page — Form and POST")
    class AiPageForm {

        @Test
        @DisplayName("AI page loads with form")
        void aiPageLoads() throws Exception {
            mockMvc.perform(get("/ai").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Grounded AI Orchestration")))
                    .andExpect(content().string(containsString("Question")))
                    .andExpect(content().string(containsString("Run retrieval-aware answer")));
        }

        @Test
        @DisplayName("AI POST returns result page")
        void aiPostReturnsPage() throws Exception {
            mockMvc.perform(post("/ai")
                    .session(session)
                    .param("question", "What is this document about?")
                    .param("scope", "ALL_DOCUMENTS"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Grounded AI Orchestration")));
        }
    }

    @Nested
    @DisplayName("Jobs — Pagination Support")
    class JobsPagination {

        @Test
        @DisplayName("jobs page loads with status filter")
        void jobsLoadsWithFilter() throws Exception {
            mockMvc.perform(get("/jobs").param("status", "PENDING").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ingestion Jobs")));
        }

        @Test
        @DisplayName("jobs page supports pagination")
        void jobsSupportsPagination() throws Exception {
            mockMvc.perform(get("/jobs").param("page", "1").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ingestion Jobs")));
        }
    }

    @Nested
    @DisplayName("Chunks Inspection — Real Query")
    class ChunksInspection {

        @Test
        @DisplayName("chunks page shows prompt when no document ID")
        void chunksShowsPrompt() throws Exception {
            mockMvc.perform(get("/chunks").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Chunk Inspection")))
                    .andExpect(content().string(containsString("Enter a document UUID")));
        }

        @Test
        @DisplayName("chunks page handles invalid UUID gracefully")
        void chunksInvalidUuid() throws Exception {
            mockMvc.perform(get("/chunks").param("documentId", "not-a-uuid").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Invalid document ID")));
        }

        @Test
        @DisplayName("chunks page queries for non-existent document")
        void chunksNonExistentDocument() throws Exception {
            mockMvc.perform(get("/chunks")
                    .param("documentId", "00000000-0000-0000-0000-000000000000")
                    .session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Chunk Inspection")));
        }
    }
}
