package com.cognitera.platform.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "platform.auth.jwt-secret=test-secret-that-is-at-least-32-bytes-long-for-hs256"
})
@AutoConfigureMockMvc
class NavigationMenuTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void faviconReturns200() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk());
    }

    @Test
    void allNavPagesReturn200AfterLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"navtest@test.com","password":"test123","displayName":"Nav Test","roles":[]}
                    """))
                .andExpect(status().isCreated());

        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "navtest@test.com")
                .param("password", "test123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/documents").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/documents/upload").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/search").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/chunks").session(session))
                .andExpect(status().isOk());


        mockMvc.perform(get("/audit").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/workspaces").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ai").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void languageSwitchWorksAfterLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"langtest@test.com","password":"test123","displayName":"Lang Test","roles":[]}
                    """))
                .andExpect(status().isCreated());

        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "langtest@test.com")
                .param("password", "test123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();

        mockMvc.perform(get("/dashboard?lang=de").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ubersicht")));
    }

    @Test
    void unauthenticatedNavPagesRedirectToLogin() throws Exception {
        String[] protectedPages = {
            "/dashboard", "/documents", "/jobs",
            "/search", "/chunks", "/audit", "/workspaces", "/ai"
        };

        for (String page : protectedPages) {
            mockMvc.perform(get(page).header("Accept", "text/html"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Test
    void logoutRedirectsToLoginPage() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"logouttest@test.com","password":"test123","displayName":"Logout Test","roles":[]}
                    """))
                .andExpect(status().isCreated());

        var result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "logouttest@test.com")
                .param("password", "test123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void registerAndLoginViaFormRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"formtest@test.com","password":"test123","displayName":"Form Test","roles":[]}
                    """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "formtest@test.com")
                .param("password", "test123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
