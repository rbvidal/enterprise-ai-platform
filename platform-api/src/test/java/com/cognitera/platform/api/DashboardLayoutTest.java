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

import static org.junit.jupiter.api.Assertions.*;
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
@DisplayName("Dashboard Card Layout Tests")
class DashboardLayoutTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"layouttest@test.com","password":"Pass1234!","displayName":"Layout Tester","roles":[]}
                    """));
        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "layouttest@test.com")
                .param("password", "Pass1234!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        session = (MockHttpSession) result.getRequest().getSession();
    }

    @Nested
    @DisplayName("Metric card HTML structure")
    class CardStructure {

        @Test
        @DisplayName("all 3 metric cards are div elements, not anchor tags")
        void allMetricCardsAreDivs() throws Exception {
            String body = mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            int divMetricCount = countOccurrences(body, "<div class=\"metric\"");
            int anchorMetricCount = countOccurrences(body, "<a href=\"/knowledge\" class=\"metric");

            assertEquals(3, divMetricCount,
                    "Expected 3 <div class=\"metric\"> cards, found " + divMetricCount);
            assertEquals(0, anchorMetricCount,
                    "Expected 0 <a> metric cards, found " + anchorMetricCount);
        }
    }

    @Nested
    @DisplayName("Column layout")
    class ColumnLayout {

        @Test
        @DisplayName("3 cards use col-6 col-md-4 without offset")
        void standardColumnCards() throws Exception {
            String body = mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            int count = countOccurrences(body, "col-6 col-md-4\"><div class=\"metric\"");
            assertEquals(3, count,
                    "Expected 3 cards with 'col-6 col-md-4', found " + count);
        }

        @Test
        @DisplayName("total 3 metric cards in the dashboard row")
        void totalThreeMetricCards() throws Exception {
            String body = mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            int basicCols = countOccurrences(body, "col-6 col-md-4\"><div class=\"metric\"");
            assertEquals(3, basicCols,
                    "Expected 3 total metric cards, found " + basicCols);
        }

        @Test
        @DisplayName("no col-lg class used (would cause unpredictable auto-width on large screens)")
        void noColLgClass() throws Exception {
            String body = mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            int colLgCount = countOccurrences(body, "col-lg");
            assertEquals(0, colLgCount,
                    "Expected 0 'col-lg' classes, found " + colLgCount);
        }
    }

    @Nested
    @DisplayName("Content completeness")
    class ContentCompleteness {

        @Test
        @DisplayName("all 3 metric labels and audit section are present")
        void allSectionsPresent() throws Exception {
            String body = mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertTrue(body.contains(">Documents<"), "Should contain Documents card");
            assertTrue(body.contains(">Ready<"), "Should contain Ready card");
            assertTrue(body.contains(">Ingestion jobs<"), "Should contain Ingestion jobs card");
            assertTrue(body.contains("Recent audit events"), "Should contain audit section");
        }
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
