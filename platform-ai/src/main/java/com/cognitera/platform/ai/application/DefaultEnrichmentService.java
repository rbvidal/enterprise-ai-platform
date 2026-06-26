package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.ChatCompletionProvider;
import com.cognitera.platform.ai.api.EnrichmentService;
import com.cognitera.platform.ai.model.EnrichmentContext;
import com.cognitera.platform.ai.model.ModelCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default semantic enrichment implementation.
 * Uses a combination of regex-based entity extraction and optional LLM-based enrichment.
 * When a ChatCompletionProvider is available, uses the LLM for higher-quality extraction.
 * Otherwise falls back to regex pattern matching.
 */
@Service
public class DefaultEnrichmentService implements EnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEnrichmentService.class);

    // Common entity patterns
    private static final Pattern ORGANIZATION = Pattern.compile(
            "\\b([A-Z][a-z]*(?:\\s+(?:&|and|of|the|in|on|at|for|to)\\s+)?[A-Z][a-z]*){1,6}\\b\\s*(?:Inc\\.?|Corp\\.?|LLC|Ltd\\.?|GmbH|AG|SA|PLC|Group|Company|Organization|Agency|Commission|Authority|Institute|University|College)");
    private static final Pattern PERSON = Pattern.compile(
            "\\b(?:Mr\\.|Mrs\\.|Ms\\.|Dr\\.|Prof\\.)\\s+[A-Z][a-z]+\\s+[A-Z][a-z]+\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern DATE = Pattern.compile(
            "\\b\\d{4}-\\d{2}-\\d{2}\\b|\\b\\d{2}/\\d{2}/\\d{4}\\b|\\b(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4}\\b");
    private static final Pattern MONEY = Pattern.compile(
            "\\b(?:\\$|EUR|USD|€|£)\\s*[\\d,]+(?:\\.\\d{2})?\\b|\\b[\\d,]+(?:\\.\\d{2})?\\s*(?:dollars|euros?|USD|EUR)\\b", Pattern.CASE_INSENSITIVE);

    private static final int MAX_TEXT_LENGTH = 8000;

    private final Optional<ChatCompletionProvider> llmProvider;

    public DefaultEnrichmentService(Optional<ChatCompletionProvider> llmProvider) {
        this.llmProvider = llmProvider;
    }

    @Override
    public EnrichmentContext enrich(String documentId, String text) {
        EnrichmentContext ctx = new EnrichmentContext(documentId);
        if (text == null || text.isBlank()) return ctx;

        String sample = text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;

        // If LLM is available, use it for high-quality extraction
        if (llmProvider.isPresent() && llmProvider.get().isAvailable()) {
            try {
                enrichWithLLM(ctx, documentId, sample);
            } catch (Exception e) {
                log.warn("LLM enrichment failed, falling back to regex: {}", e.getMessage());
                enrichWithRegex(ctx, documentId, sample);
            }
        } else {
            enrichWithRegex(ctx, documentId, sample);
        }

        log.debug("Enriched document {}: {} entities, {} concepts, {} relationships",
                documentId, ctx.getEntities().size(), ctx.getConcepts().size(), ctx.getRelationships().size());
        return ctx;
    }

    private void enrichWithLLM(EnrichmentContext ctx, String documentId, String text) {
        String prompt = buildEnrichmentPrompt(text);
        String response = llmProvider.get().complete(prompt, new ModelCapabilities("enrichment", "local", 4096, false, false, true));
        parseLLMResponse(ctx, response);
    }

    private void enrichWithRegex(EnrichmentContext ctx, String documentId, String text) {
        // Extract organizations
        Matcher m = ORGANIZATION.matcher(text);
        Set<String> seenOrgs = new HashSet<>();
        while (m.find()) {
            String name = m.group().trim();
            if (seenOrgs.add(name.toLowerCase()) && name.length() > 3) {
                ctx.addEntity(new EnrichmentContext.ExtractedEntity(
                        name, "ORGANIZATION", 0.7, List.of(m.group()), extractContext(text, m.start())));
            }
        }

        // Extract persons
        m = PERSON.matcher(text);
        Set<String> seenPersons = new HashSet<>();
        while (m.find()) {
            String name = m.group().trim();
            if (seenPersons.add(name.toLowerCase())) {
                ctx.addEntity(new EnrichmentContext.ExtractedEntity(
                        name, "PERSON", 0.8, List.of(m.group()), extractContext(text, m.start())));
            }
        }

        // Extract dates as temporal concepts
        m = DATE.matcher(text);
        Set<String> seenDates = new HashSet<>();
        while (m.find() && seenDates.size() < 20) {
            String date = m.group().trim();
            if (seenDates.add(date)) {
                ctx.addConcept(new EnrichmentContext.ExtractedConcept(
                        "Date: " + date, "temporal", 0.9, List.of()));
            }
        }

        // Extract monetary values
        m = MONEY.matcher(text);
        int moneyCount = 0;
        while (m.find() && moneyCount < 10) {
            ctx.addConcept(new EnrichmentContext.ExtractedConcept(
                    "Amount: " + m.group().trim(), "financial", 0.85, List.of()));
            moneyCount++;
        }
    }

    private void parseLLMResponse(EnrichmentContext ctx, String response) {
        // Best-effort parsing of LLM JSON response
        // In production, structured output (JSON mode / tool calling) should be used
        if (response == null || response.isBlank()) return;

        try {
            // Extract entities section
            if (response.contains("\"entities\"")) {
                String[] entityBlocks = response.split("\\{[^}]*\"name\"[^}]*\\}");
                for (String block : entityBlocks) {
                    String name = extractJsonString(block, "name");
                    String type = extractJsonString(block, "type");
                    if (name != null && !name.isBlank()) {
                        ctx.addEntity(new EnrichmentContext.ExtractedEntity(
                                name, type != null ? type : "unknown", 0.8, List.of(name), ""));
                    }
                }
            }

            // Extract concepts
            if (response.contains("\"concepts\"")) {
                String[] conceptBlocks = response.split("\\{[^}]*\"label\"[^}]*\\}");
                for (String block : conceptBlocks) {
                    String label = extractJsonString(block, "label");
                    String domain = extractJsonString(block, "domain");
                    if (label != null && !label.isBlank()) {
                        ctx.addConcept(new EnrichmentContext.ExtractedConcept(
                                label, domain != null ? domain : "general", 0.7, List.of()));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse LLM enrichment response: {}", e.getMessage());
        }
    }

    private String buildEnrichmentPrompt(String text) {
        return """
            Extract structured information from the following document text.
            Return a JSON object with two arrays:
            - "entities": objects with "name", "type" (ORGANIZATION, PERSON, TECHNOLOGY, REGULATION, PROJECT), and "mentions"
            - "concepts": objects with "label", "domain", and "confidence"
            - "relationships": objects with "sourceId", "targetId", "type" (REFERENCES, PART_OF, RELATED_TO), and "evidence"

            Focus on:
            - Organizations, companies, agencies
            - People mentioned
            - Technologies, products, platforms
            - Regulations, standards, policies
            - Key concepts and topics
            - Relationships between entities

            TEXT:
            %s
            """.formatted(text);
    }

    private static String extractJsonString(String block, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(block);
        return m.find() ? m.group(1) : null;
    }

    private static String extractContext(String text, int position) {
        int start = Math.max(0, position - 40);
        int end = Math.min(text.length(), position + 80);
        return text.substring(start, end).replace('\n', ' ');
    }
}
