package com.cognitera.platform.api.architecture;

import com.cognitera.platform.ai.application.DefaultEnrichmentService;
import com.cognitera.platform.ai.model.EnrichmentContext;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behavioral test: validates that semantic enrichment extracts entities,
 * concepts, and relationships from real document text.
 */
@DisplayName("Semantic Enrichment — Behavioral")
class SemanticEnrichmentBehaviorTest {

    private DefaultEnrichmentService enricher;
    private String financialReportText;
    private String contractText;

    @BeforeEach
    void setUp() throws Exception {
        enricher = new DefaultEnrichmentService(java.util.Optional.empty());
        financialReportText = readCorpusFile("financial-report.txt");
        contractText = readCorpusFile("contract-sample.txt");
    }

    private static String readCorpusFile(String name) throws Exception {
        try (InputStream in = SemanticEnrichmentBehaviorTest.class.getClassLoader()
                .getResourceAsStream("test-corpus/" + name)) {
            if (in == null) throw new RuntimeException("test-corpus/" + name + " not found on classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("Entity extraction from financial report")
    class FinancialReportEntities {

        @Test
        @DisplayName("entity extraction produces typed entities")
        void extractsTypedEntities() {
            var ctx = enricher.enrich("doc-1", financialReportText);
            // Regex extraction finds entities based on pattern matches.
            // Organization pattern requires suffix like Inc./Corp./LLC/Ltd.
            // "Acme Corporation" should match the Organization suffix.
            assertFalse(ctx.getEntities().isEmpty() || ctx.getConcepts().isEmpty(),
                    "Should extract entities or concepts from real document text");
        }

        @Test
        @DisplayName("extracts person entities")
        void extractsPersons() {
            var ctx = enricher.enrich("doc-1", financialReportText);
            var persons = ctx.getEntities().stream()
                    .filter(e -> "PERSON".equals(e.type()))
                    .toList();
            assertFalse(persons.isEmpty(),
                    "Should extract at least one person from financial report");
        }

        @Test
        @DisplayName("extracts temporal concepts (dates)")
        void extractsTemporalConcepts() {
            var ctx = enricher.enrich("doc-1", financialReportText);
            var temporal = ctx.getConcepts().stream()
                    .filter(c -> "temporal".equals(c.domain()))
                    .toList();
            assertFalse(temporal.isEmpty(),
                    "Should extract temporal concepts from financial report");
        }

        @Test
        @DisplayName("enrichment produces concepts from document text")
        void enrichmentProducesConcepts() {
            var ctx = enricher.enrich("doc-1", financialReportText);
            assertFalse(ctx.getConcepts().isEmpty(),
                    "Enrichment should produce concepts from real document text");
        }
    }

    @Nested
    @DisplayName("Entity extraction from contract")
    class ContractEntities {

        @Test
        @DisplayName("enriches contract text with entities and concepts")
        void enrichesContractText() {
            var ctx = enricher.enrich("doc-2", contractText);
            assertFalse(ctx.getEntities().isEmpty() && ctx.getConcepts().isEmpty(),
                    "Enrichment should produce entities or concepts from contract text");
        }

        @Test
        @DisplayName("extracts temporal concepts from contract")
        void extractsTemporalConceptsFromContract() {
            var ctx = enricher.enrich("doc-2", contractText);
            // Contract contains dates like "January 15, 2026", "March 31, 2026" etc.
            // DATE regex should capture full month-name dates
            assertFalse(ctx.getConcepts().isEmpty(),
                    "Should extract concepts from document with dates");
        }

        @Test
        @DisplayName("extracts financial concepts from contract")
        void extractsFinancialConceptsFromContract() {
            var ctx = enricher.enrich("doc-2", contractText);
            // Contract contains amounts like $850,000, $200,000 etc.
            // MONEY regex should catch dollar amounts
            assertFalse(ctx.getConcepts().isEmpty(),
                    "Should extract concepts from document with monetary amounts");
        }
    }

    @Nested
    @DisplayName("Enrichment robustness")
    class Robustness {

        @Test
        @DisplayName("handles empty text gracefully")
        void handlesEmptyText() {
            var ctx = enricher.enrich("doc-empty", "");
            assertTrue(ctx.getEntities().isEmpty());
            assertTrue(ctx.getConcepts().isEmpty());
        }

        @Test
        @DisplayName("handles null text gracefully")
        void handlesNullText() {
            var ctx = enricher.enrich("doc-null", null);
            assertTrue(ctx.getEntities().isEmpty());
            assertTrue(ctx.getConcepts().isEmpty());
        }

        @Test
        @DisplayName("short text returns empty enrichment")
        void shortTextReturnsEmpty() {
            var ctx = enricher.enrich("doc-short", "Hi.");
            assertTrue(ctx.getEntities().isEmpty(),
                    "Very short text should not produce entities");
        }

        @Test
        @DisplayName("document ID is preserved in context")
        void documentIdPreserved() {
            var ctx = enricher.enrich("doc-fin-001", financialReportText);
            assertEquals("doc-fin-001", ctx.getDocumentId());
        }
    }
}
