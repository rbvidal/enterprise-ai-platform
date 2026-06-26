package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.QueryIntentClassifier;
import com.cognitera.platform.ai.model.QueryIntent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Classifies query intent by matching keywords against predefined intent patterns.
 */
@Component
public class DefaultQueryIntentClassifier implements QueryIntentClassifier {

    private static final Map<QueryIntent, Set<String>> INTENT_PATTERNS = Map.of(
            QueryIntent.INDEX_INSPECTION, Set.of(
                    "does your index contain", "is there information in the index",
                    "what does the index contain", "is the corpus",
                    "is there material", "do you have information",
                    "do you have documents", "what documents do you have",
                    "does the system contain", "is stored in the",
                    "what topics", "which documents are"),
            QueryIntent.CORPUS_DISCOVERY, Set.of(
                    "what do you know about", "what can you tell me about",
                    "what areas of", "what topics are covered",
                    "what references", "which documents",
                    "show me all documents"),
            QueryIntent.DOCUMENT_RESEARCH, Set.of(
                    "what does", "what is", "explain",
                    "definition", "meaning of",
                    "tell me about section", "tell me about reference"),
            QueryIntent.DOCUMENT_LOOKUP, Set.of(
                    "find me", "show me the document",
                    "where is the document", "locate",
                    "search for the document"),
            QueryIntent.SOURCE_ANALYSIS, Set.of(
                    "analyze this source", "what does this document prove",
                    "what does the email", "what does the letter",
                    "is this sufficient evidence"),
            QueryIntent.WORKSPACE_ANALYSIS, Set.of(
                    "analyze my workspace", "my documents",
                    "what should I do about my", "given my situation"),
            QueryIntent.QUESTION_ANSWERING, Set.of(
                    "what can i do", "what should i do", "what are my rights",
                    "am i allowed", "can i",
                    "how to proceed", "next steps", "procedure",
                    "what happens if", "is it legal")
    );

    @Override
    public QueryIntent classify(String query) {
        String lower = query.toLowerCase().trim();

        int bestScore = -1;
        QueryIntent bestIntent = QueryIntent.QUESTION_ANSWERING;

        for (var entry : INTENT_PATTERNS.entrySet()) {
            int score = 0;
            for (String pattern : entry.getValue()) {
                if (lower.contains(pattern)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestIntent = entry.getKey();
            }
        }

        return bestIntent;
    }
}
