package com.cognitera.platform.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@DisplayName("Page Access and Template Rendering Tests")
class PageAccessTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"pagetester@test.com","password":"Pass1234!","displayName":"Page Tester","roles":[]}
                    """));

        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "pagetester@test.com")
                .param("password", "Pass1234!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        session = (MockHttpSession) result.getRequest().getSession();
    }

    @Nested
    @DisplayName("Public Pages (unauthenticated)")
    class PublicPages {

        @ParameterizedTest
        @ValueSource(strings = {"/login", "/register"})
        @DisplayName("should return 200 for public pages")
        void publicPagesReturn200(String path) throws Exception {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("login page contains login form")
        void loginPageHasForm() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Email")))
                    .andExpect(content().string(containsString("Password")));
        }

        @Test
        @DisplayName("register page contains registration form")
        void registerPageHasForm() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Register")))
                    .andExpect(content().string(containsString("Display name")));
        }
    }

    @Nested
    @DisplayName("Protected Pages (authenticated)")
    class ProtectedPages {

        @ParameterizedTest
        @ValueSource(strings = {
            "/dashboard", "/documents", "/documents/upload",
            "/jobs", "/search", "/chunks",
            "/audit", "/workspaces",
            "/workspaces/new", "/ai"
        })
        @DisplayName("should return 200 for all authenticated pages")
        void allProtectedPagesReturn200(String path) throws Exception {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("dashboard shows key sections")
        void dashboardContent() throws Exception {
            mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Dashboard")))
                    .andExpect(content().string(containsString("Recent audit events")));
        }

        @Test
        @DisplayName("AI page shows provider status and model selection")
        void aiPageContent() throws Exception {
            mockMvc.perform(get("/ai").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Provider Status")))
                    .andExpect(content().string(containsString("modelSelect")));
        }

        @Test
        @DisplayName("search page shows search form")
        void searchPageContent() throws Exception {
            mockMvc.perform(get("/search").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Document Search")))
                    .andExpect(content().string(containsString("hybrid")));
        }

        @Test
        @DisplayName("chunks page shows inspection form")
        void chunksPageContent() throws Exception {
            mockMvc.perform(get("/chunks").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Chunk Inspection")));
        }

        @Test
        @DisplayName("jobs page shows table headers")
        void jobsPageContent() throws Exception {
            mockMvc.perform(get("/jobs").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Ingestion Jobs")))
                    .andExpect(content().string(containsString("Status")));
        }

        @Test
        @DisplayName("audit page shows filter form")
        void auditPageContent() throws Exception {
            mockMvc.perform(get("/audit").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Audit Viewer")))
                    .andExpect(content().string(containsString("Any event")));
        }

        @Test
        @DisplayName("workspace creation page shows stage indicator")
        void workspaceCreationContent() throws Exception {
            mockMvc.perform(get("/workspaces/new").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Stage 1 of 9")));
        }
    }

    @Nested
    @DisplayName("Error Pages")
    class ErrorPages {

        @Test
        @DisplayName("unknown page returns error status")
        void unknownPage() throws Exception {
            mockMvc.perform(get("/nonexistent-page").session(session))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("favicon is accessible")
        void faviconAccessible() throws Exception {
            mockMvc.perform(get("/favicon.ico"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("CSS is accessible")
        void cssAccessible() throws Exception {
            mockMvc.perform(get("/css/platform.css"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Redirect Security")
    class Redirects {

        @ParameterizedTest
        @ValueSource(strings = {
            "/dashboard", "/documents", "/documents/upload",
            "/jobs", "/search", "/chunks",
            "/audit", "/workspaces",
            "/workspaces/new", "/ai"
        })
        @DisplayName("should redirect unauthenticated users to login")
        void unauthenticatedRedirect(String path) throws Exception {
            mockMvc.perform(get(path).header("Accept", "text/html"))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
