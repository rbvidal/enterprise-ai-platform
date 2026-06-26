package com.cognitera.platform.ai.model;

/**
 * A single message in an AI conversation with a role and text content.
 */
public record AiMessage(
        AiRole role,
        String content
) {
}
