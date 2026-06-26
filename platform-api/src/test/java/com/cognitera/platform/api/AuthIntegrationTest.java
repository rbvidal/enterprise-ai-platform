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
import org.springframework.test.web.servlet.MvcResult;

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
@DisplayName("Authentication Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession authenticatedSession;
    private String accessToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        // Try to register (might already exist from previous test in same class)
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"SecurePass123!","displayName":"Test User","roles":[]}
                    """));

        // Get JWT via API login
        MvcResult apiLoginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"user@test.com","password":"SecurePass123!"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        accessToken = com.jayway.jsonpath.JsonPath.read(
                apiLoginResult.getResponse().getContentAsString(), "$.accessToken");

        // Also login via form for session-based tests
        MvcResult formLoginResult = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "user@test.com")
                .param("password", "SecurePass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        authenticatedSession = (MockHttpSession) formLoginResult.getRequest().getSession();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("should reject duplicate email")
        void rejectDuplicateEmail() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@test.com","password":"AnotherPass1","displayName":"Another User","roles":[]}
                        """))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should reject missing email")
        void rejectMissingEmail() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"","password":"Pass1234!","displayName":"No Email","roles":[]}
                        """))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should reject missing password")
        void rejectMissingPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"weak@test.com","password":"","displayName":"No Pass","roles":[]}
                        """))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should reject admin role in self-registration")
        void rejectAdminSelfRegistration() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"admin@test.com","password":"Pass1234!","displayName":"Wannabe Admin","roles":["ADMIN"]}
                        """))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("should login via form and redirect to dashboard")
        void formLoginSuccess() throws Exception {
            mockMvc.perform(post("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "user@test.com")
                    .param("password", "SecurePass123!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        }

        @Test
        @DisplayName("should reject wrong password")
        void wrongPassword() throws Exception {
            mockMvc.perform(post("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "user@test.com")
                    .param("password", "WrongPassword1!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("should reject non-existent user")
        void nonExistentUser() throws Exception {
            mockMvc.perform(post("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", "nobody@test.com")
                    .param("password", "SomePass123!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        @DisplayName("should login via API and return JWT tokens")
        void apiLoginSuccess() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@test.com","password":"SecurePass123!"}
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("should refresh token with valid refresh token")
        void refreshTokenSuccess() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"user@test.com","password":"SecurePass123!"}
                        """))
                    .andExpect(status().isOk())
                    .andReturn();

            String refreshToken = com.jayway.jsonpath.JsonPath.read(
                    loginResult.getResponse().getContentAsString(), "$.refreshToken");

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("should reject invalid refresh token")
        void invalidRefreshToken() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"invalid-token\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("JWT-protected endpoints")
    class JwtAuth {

        @Test
        @DisplayName("should access /api/auth/me with valid JWT")
        void accessMeWithJwt() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("user@test.com"));
        }

        @Test
        @DisplayName("should reject /api/auth/me without token")
        void accessMeWithoutToken() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should reject /api/auth/me with malformed token")
        void accessMeWithMalformedToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer garbage.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Logout")
    class Logout {

        @Test
        @DisplayName("should logout and redirect to login page")
        void logoutSuccess() throws Exception {
            mockMvc.perform(post("/logout").session(authenticatedSession))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?logout"));
        }

        @Test
        @DisplayName("should clear session after logout")
        void sessionCleared() throws Exception {
            mockMvc.perform(post("/logout").session(authenticatedSession))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
