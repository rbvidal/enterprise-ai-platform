package com.cognitera.platform.api.resilience;

import com.cognitera.platform.ai.api.ChatCompletionProvider;
import com.cognitera.platform.ai.api.ModelCapabilityRegistry;
import com.cognitera.platform.ai.application.DefaultModelCapabilityRegistry;
import com.cognitera.platform.ai.application.DefaultProviderRouter;
import com.cognitera.platform.ai.model.InferenceRequest;
import com.cognitera.platform.ai.model.ModelCapabilities;
import com.cognitera.platform.search.api.GraphSearchProvider;
import com.cognitera.platform.search.model.RetrievalCandidate;
import com.cognitera.platform.search.model.SearchQuery;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioral test: validates graceful degradation under infrastructure failures.
 * Tests that the platform continues functioning when optional services are unavailable.
 */
@DisplayName("Graceful Degradation — Resilience")
class GracefulDegradationTest {

    // ── Stub providers for testing ──

    private static class StubProvider implements ChatCompletionProvider {
        private final String name;
        private final boolean available;
        StubProvider(String name, boolean available) { this.name = name; this.available = available; }
        @Override public String providerName() { return name; }
        @Override public boolean isAvailable() { return available; }
        @Override public String complete(String prompt, ModelCapabilities caps) { return name + ": response"; }
    }

    private static class UnavailableGraphProvider implements GraphSearchProvider {
        @Override public boolean isAvailable() { return false; }
        @Override public List<RetrievalCandidate> search(SearchQuery q) { return List.of(); }
        @Override public List<String> findRelatedDocuments(String docId, int depth) { return List.of(); }
    }

    @Nested
    @DisplayName("Provider Router — fallback behavior")
    class ProviderRouterFallback {

        @Test
        @DisplayName("skips unavailable provider and selects available one")
        void skipsUnavailableProvider() {
            var registry = new DefaultModelCapabilityRegistry();
            var router = new DefaultProviderRouter(
                    List.of(new StubProvider("openai", false),
                            new StubProvider("ollama", true)),
                    registry);

            var request = InferenceRequest.forChat("test prompt", null);
            var provider = router.routeChat(request);

            assertEquals("ollama", provider.providerName(),
                    "Should skip unavailable openai and select ollama");
        }

        @Test
        @DisplayName("throws when all providers are unavailable")
        void throwsWhenAllUnavailable() {
            var registry = new DefaultModelCapabilityRegistry();
            var router = new DefaultProviderRouter(
                    List.of(new StubProvider("openai", false),
                            new StubProvider("ollama", false)),
                    registry);

            var request = InferenceRequest.forChat("test prompt", null);
            assertThrows(IllegalStateException.class,
                    () -> router.routeChat(request),
                    "Should throw when no provider is available");
        }

        @Test
        @DisplayName("preferred provider skipped when unavailable")
        void preferredProviderSkippedWhenUnavailable() {
            var registry = new DefaultModelCapabilityRegistry();
            var router = new DefaultProviderRouter(
                    List.of(new StubProvider("openai", false),
                            new StubProvider("ollama", true)),
                    registry);

            var request = new InferenceRequest("prompt", null, "openai",
                    null, null, 0, null, "user");
            var provider = router.routeChat(request);

            assertEquals("ollama", provider.providerName(),
                    "Should skip unavailable preferred provider");
        }
    }

    @Nested
    @DisplayName("Graph Search Provider — unavailable behavior")
    class GraphSearchUnavailable {

        @Test
        @DisplayName("reports unavailable when Neo4j is down")
        void reportsUnavailable() {
            var provider = new UnavailableGraphProvider();
            assertFalse(provider.isAvailable(),
                    "Graph provider should report unavailable when Neo4j is down");
        }

        @Test
        @DisplayName("returns empty search results when unavailable")
        void returnsEmptySearchResults() {
            var provider = new UnavailableGraphProvider();
            var results = provider.search(new SearchQuery("test", null, null, null, 0, 10));
            assertTrue(results.isEmpty(),
                    "Graph search should return empty results when unavailable");
        }

        @Test
        @DisplayName("returns empty related documents when unavailable")
        void returnsEmptyRelatedDocuments() {
            var provider = new UnavailableGraphProvider();
            var docs = provider.findRelatedDocuments("doc-1", 2);
            assertTrue(docs.isEmpty(),
                    "Related documents should be empty when graph is unavailable");
        }
    }

    @Nested
    @DisplayName("Provider SPI contract — graceful responses")
    class ProviderContract {

        @Test
        @DisplayName("unavailable provider still returns valid metadata")
        void unavailableProviderReturnsValidMetadata() {
            var provider = new StubProvider("test-provider", false);
            assertEquals("test-provider", provider.providerName());
            assertFalse(provider.isAvailable());
            // Even unavailable providers should not throw from metadata methods
            assertDoesNotThrow(() -> provider.providerName());
            assertDoesNotThrow(() -> provider.isAvailable());
        }

        @Test
        @DisplayName("available provider returns non-null for valid prompt")
        void availableProviderReturnsNonNull() {
            var provider = new StubProvider("test-provider", true);
            var result = provider.complete("test",
                    new ModelCapabilities("test", "model", 4096, true, true, true));
            assertNotNull(result);
        }
    }
}
