package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.PromptRegistry;
import com.cognitera.platform.ai.model.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory prompt registry seeded with platform prompt templates.
 * In production, prompts would be loaded from YAML/JSON resources or a database.
 */
@Component
public class DefaultPromptRegistry implements PromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultPromptRegistry.class);

    private final Map<String, List<PromptTemplate>> prompts = new ConcurrentHashMap<>();

    public DefaultPromptRegistry() {
        registerDefaultPrompts();
    }

    @Override
    public Optional<PromptTemplate> get(String qualifiedId) {
        int slash = qualifiedId.lastIndexOf("/v");
        if (slash < 0) return Optional.empty();
        String promptId = qualifiedId.substring(0, slash);
        try {
            int version = Integer.parseInt(qualifiedId.substring(slash + 2));
            return getVersions(promptId).stream()
                    .filter(p -> p.getVersion() == version)
                    .findFirst();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PromptTemplate> getLatest(String promptId) {
        return getVersions(promptId).stream()
                .max(Comparator.comparingInt(PromptTemplate::getVersion));
    }

    @Override
    public List<String> listPromptIds() {
        return List.copyOf(prompts.keySet());
    }

    @Override
    public List<PromptTemplate> getVersions(String promptId) {
        return prompts.getOrDefault(promptId, List.of());
    }

    @Override
    public void register(PromptTemplate template) {
        prompts.computeIfAbsent(template.getId(), k -> new ArrayList<>()).add(template);
        log.debug("Registered prompt: {}", template.getQualifiedId());
    }

    private void registerDefaultPrompts() {
        register(new PromptTemplate("rag-answer", 1,
                "RAG-grounded answer generation for document intelligence queries",
                """
                You are an AI assistant analyzing real documents.
                Base your answer ONLY on the retrieved context below.
                Cite specific sources using bracket notation [1], [2], etc.
                If the context does not contain enough information, say so explicitly.

                RETRIEVED CONTEXT:
                {{context}}

                QUESTION:
                {{question}}

                INSTRUCTIONS:
                - Cite sources for every factual claim
                - Distinguish between direct evidence and inference
                - Flag any temporal inconsistencies
                - If information is missing, state what additional documents would help
                """,
                List.of("context", "question"),
                "text", List.of("*"), Map.of("type", "rag", "domain", "general")));

        register(new PromptTemplate("entity-extraction", 1,
                "Extract structured entities and concepts from document text",
                """
                Extract structured information from the following document text.
                Return a JSON object with:
                - "entities": objects with "name", "type" (ORGANIZATION, PERSON, TECHNOLOGY, REGULATION, PROJECT)
                - "concepts": objects with "label", "domain", "confidence"
                - "relationships": objects with "sourceId", "targetId", "type", "evidence"

                TEXT:
                {{text}}
                """,
                List.of("text"), "json", List.of("*"),
                Map.of("type", "extraction", "domain", "general")));

        register(new PromptTemplate("rerank-evaluation", 1,
                "Score document excerpts for relevance to a query",
                """
                You are a relevance scoring engine.
                For each of the following document excerpts, score how relevant it is to the query.
                Score from 0 (completely irrelevant) to 10 (directly answers the query).

                QUERY:
                {{query}}

                EXCERPTS:
                {{excerpts}}

                Return one line per excerpt in format: index=score
                """,
                List.of("query", "excerpts"), "scored-list", List.of("*"),
                Map.of("type", "evaluation", "domain", "general")));
    }
}
