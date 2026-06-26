package com.cognitera.platform.search.application;

import com.cognitera.platform.search.api.ChunkingStrategy;
import com.cognitera.platform.search.model.ChunkMetadata;
import com.cognitera.platform.search.model.ChunkPosition;
import com.cognitera.platform.search.model.ChunkType;
import com.cognitera.platform.search.model.DocumentChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chunks text at sentence boundaries with configurable target chunk size and overlap. */
public class SentenceAwareChunkingStrategy implements ChunkingStrategy {

    private static final int TARGET_CHUNK_SIZE = 1200;
    private static final int MIN_CHUNK_SIZE = 200;
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?])\\s+(?=[A-Z\\u0410-\\u042F\\n])|(?<=\\n\\n)|(?<=[:;])\\s+(?=[A-Z\\u0410-\\u042F])");

    @Override
    public List<DocumentChunk> chunk(UUID documentId, int documentVersion, String title, String text) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<Integer> boundaries = findSentenceBoundaries(normalized);
        List<DocumentChunk> chunks = new ArrayList<>();
        int start = 0;
        int chunkIndex = 0;

        while (start < normalized.length()) {
            int idealEnd = Math.min(start + TARGET_CHUNK_SIZE, normalized.length());
            int end = findBestSplitPoint(normalized, boundaries, start, idealEnd);
            String slice = normalized.substring(start, end).trim();
            if (!slice.isBlank()) {
                chunks.add(new DocumentChunk(
                        UUID.randomUUID(),
                        documentId,
                        documentVersion,
                        ChunkType.TEXT,
                        slice,
                        new ChunkPosition(null, null, chunkIndex, start, end),
                        new ChunkMetadata(title, null, null, null, null, null, null, null, null),
                        null,
                        null
                ));
                chunkIndex++;
            }
            if (end >= normalized.length()) {
                break;
            }
            // Advance start, with overlap
            start = Math.max(start + TARGET_CHUNK_SIZE - 150, findPreviousBoundary(boundaries, end));
            start = Math.min(start, normalized.length());
        }
        return chunks;
    }

    private List<Integer> findSentenceBoundaries(String text) {
        List<Integer> boundaries = new ArrayList<>();
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);
        while (matcher.find()) {
            boundaries.add(matcher.start() + 1);
        }
        return boundaries;
    }

    private int findBestSplitPoint(String text, List<Integer> boundaries, int start, int idealEnd) {
        if (idealEnd >= text.length()) {
            return text.length();
        }
        // Prefer splitting at a sentence boundary after the minimum chunk size
        for (int boundary : boundaries) {
            if (boundary > start + MIN_CHUNK_SIZE && boundary <= idealEnd + 200) {
                return Math.min(boundary, text.length());
            }
        }
        // Fall back to splitting at a reasonable point
        int fallback = text.indexOf('\n', idealEnd - 100);
        if (fallback > start + MIN_CHUNK_SIZE && fallback < idealEnd + 200) {
            return fallback + 1;
        }
        // Last resort: just use the ideal end
        return Math.min(idealEnd, text.length());
    }

    private int findPreviousBoundary(List<Integer> boundaries, int position) {
        for (int i = boundaries.size() - 1; i >= 0; i--) {
            if (boundaries.get(i) < position) {
                return boundaries.get(i);
            }
        }
        return position;
    }
}
