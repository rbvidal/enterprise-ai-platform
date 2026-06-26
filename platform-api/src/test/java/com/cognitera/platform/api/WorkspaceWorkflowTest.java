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
@DisplayName("Workspace Workflow Tests")
class WorkspaceWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession session;
    private String createdWorkspaceId;

    @BeforeEach
    void login() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"workspacer@test.com","password":"Pass1234!","displayName":"Workspace User","roles":[]}
                    """));

        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "workspacer@test.com")
                .param("password", "Pass1234!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        session = (MockHttpSession) result.getRequest().getSession();
    }

    @Nested
    @DisplayName("Workspace Listing")
    class Listing {

        @Test
        @DisplayName("should show empty workspace list")
        void emptyWorkspaceList() throws Exception {
            mockMvc.perform(get("/workspaces").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Workspaces")));
        }

        @Test
        @DisplayName("should show workspaces after creation")
        void populatedWorkspaceList() throws Exception {
            createWorkspace();

            mockMvc.perform(get("/workspaces").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("No workspaces yet"))));
        }
    }

    @Nested
    @DisplayName("Workspace Creation")
    class Creation {

        @Test
        @DisplayName("should show creation form with populated dropdowns")
        void showCreationForm() throws Exception {
            mockMvc.perform(get("/workspaces/new").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Workspace Type")))
                    .andExpect(content().string(containsString("GENERAL")))
                    .andExpect(content().string(containsString("GENERAL")))
                    .andExpect(content().string(containsString("ANALYSIS")))
                    .andExpect(content().string(containsString("REVIEW")));
        }

        @Test
        @DisplayName("should create workspace and redirect to wizard")
        void createWorkspaceAndRedirect() throws Exception {
            createWorkspace();

            // The redirect URL should contain /workspaces/{id}/wizard
            mockMvc.perform(get("/workspaces").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(createdWorkspaceId)));
        }
    }

    @Nested
    @DisplayName("Workspace Pages")
    class Pages {

        @BeforeEach
        void setup() throws Exception {
            if (createdWorkspaceId == null) createWorkspace();
        }

        @Test
        @DisplayName("should show workspace detail page")
        void showDetailPage() throws Exception {
            mockMvc.perform(get("/workspaces/" + createdWorkspaceId).session(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should show workspace wizard page")
        void showWizardPage() throws Exception {
            mockMvc.perform(get("/workspaces/" + createdWorkspaceId + "/wizard").session(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should show workspace detail analysis page")
        void showDetailAnalysisPage() throws Exception {
            mockMvc.perform(get("/workspaces/" + createdWorkspaceId + "/detail").session(session))
                    .andExpect(status().isOk());
        }
    }

    private void createWorkspace() throws Exception {
        var result = mockMvc.perform(post("/workspaces/new")
                .session(session)
                .param("workspaceType", "GENERAL")
                .param("domain", "GENERAL")
                .param("workspaceReference", "WS-TEST-001")
                .param("additionalNotes", "Test workspace for integration testing"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        // Extract workspace ID from /workspaces/{id}/wizard
        createdWorkspaceId = location.replaceAll(".*/workspaces/([^/]+)/wizard.*", "$1");
    }
}
