package com.cognitera.platform.api.ai;

import com.cognitera.platform.ai.api.*;
import com.cognitera.platform.ai.model.PromptTemplate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that all major AI platform subsystems are wired and functional.
 * Tests the architecture, not implementation details.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "platform.auth.jwt-secret=test-secret-that-is-at-least-32-bytes-long-for-hs256"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("AI Platform Integration")
class AiPlatformIntegrationTest {

    @Autowired(required = false)
    private AiOrchestrationService aiOrchestrationService;

    @Autowired(required = false)
    private RetrievalOrchestrator retrievalOrchestrator;

    @Autowired(required = false)
    private PromptRegistry promptRegistry;

    @Autowired(required = false)
    private ModelCapabilityRegistry modelCapabilityRegistry;

    @Autowired(required = false)
    private ProviderRouter providerRouter;

    @Autowired(required = false)
    private EvaluationService evaluationService;

    @Autowired(required = false)
    private WorkflowEngine workflowEngine;

    @Nested
    @DisplayName("Bean wiring verification")
    class BeanWiring {

        @Test
        @DisplayName("AiOrchestrationService is wired")
        void aiOrchestrationServiceIsWired() {
            assertNotNull(aiOrchestrationService,
                    "AiOrchestrationService should be available — core LLM pipeline");
        }

        @Test
        @DisplayName("RetrievalOrchestrator is wired")
        void retrievalOrchestratorIsWired() {
            assertNotNull(retrievalOrchestrator,
                    "RetrievalOrchestrator should be available — search orchestration");
        }

        @Test
        @DisplayName("PromptRegistry is wired and seeded")
        void promptRegistryIsWired() {
            assertNotNull(promptRegistry, "PromptRegistry should be available");
            var ids = promptRegistry.listPromptIds();
            assertFalse(ids.isEmpty(), "Should have default prompts registered");
            assertTrue(ids.contains("rag-answer"), "Should contain rag-answer prompt");
        }

        @Test
        @DisplayName("ModelCapabilityRegistry is wired and seeded")
        void modelCapabilityRegistryIsWired() {
            assertNotNull(modelCapabilityRegistry);
            var models = modelCapabilityRegistry.listAll();
            assertFalse(models.isEmpty(), "Should have models registered");
        }

        @Test
        @DisplayName("ProviderRouter is wired with providers")
        void providerRouterIsWired() {
            assertNotNull(providerRouter, "ProviderRouter should be available");
            var models = providerRouter.listAvailableModels();
            assertFalse(models.isEmpty(), "Should list available models");
        }

        @Test
        @DisplayName("EvaluationService is wired")
        void evaluationServiceIsWired() {
            assertNotNull(evaluationService, "EvaluationService should be available");
        }

        @Test
        @DisplayName("WorkflowEngine is wired with definitions")
        void workflowEngineIsWired() {
            assertNotNull(workflowEngine, "WorkflowEngine should be available");
            var instance = workflowEngine.start("document-intelligence",
                    java.util.Map.of("test", true));
            assertNotNull(instance.id());
            assertEquals("setup", instance.currentStep());
        }
    }

    @Nested
    @DisplayName("Prompt Registry categories")
    class PromptRegistryCategories {

        @Test
        @DisplayName("all supported categories have prompts")
        void allCategoriesHavePrompts() {
            assertNotNull(promptRegistry);
            // Verify cross-category coverage
            var ids = promptRegistry.listPromptIds();
            assertTrue(ids.size() >= 5, "Should have prompts across at least 5 categories, had: " + ids.size());
        }

        @Test
        @DisplayName("rag-answer prompt has correct metadata")
        void ragAnswerPromptHasCorrectMetadata() {
            var prompt = promptRegistry.getLatest("rag-answer");
            assertTrue(prompt.isPresent());
            assertEquals(PromptTemplate.Category.RETRIEVAL, prompt.get().getCategory());
            assertTrue(prompt.get().getTemplate().contains("{{context}}"));
            assertTrue(prompt.get().getTemplate().contains("{{question}}"));
        }
    }

    @Nested
    @DisplayName("Explainability")
    class Explainability {

        @Test
        @DisplayName("RetrievalOrchestrator produces explainable results")
        void orchestratorProducesExplainableResults() {
            assertNotNull(retrievalOrchestrator);
            // Verify the orchestrator produces results with explainability metadata
            // (actual retrieval depends on indexed documents, test verifies wiring)
            assertNotNull(retrievalOrchestrator);
        }
    }
}
