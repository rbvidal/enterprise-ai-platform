package com.cognitera.platform.ai.application;

import org.springframework.stereotype.Component;

/**
 * Formats raw answer text into safe HTML with paragraph and bold formatting.
 */
@Component
public class AnswerFormatter {

    /**
     * Escapes HTML entities and converts markdown-style formatting to safe HTML paragraphs.
     */
    public String toSafeHtml(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String escaped = raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        StringBuilder html = new StringBuilder();
        String[] paragraphs = escaped.split("\n\n");
        for (int i = 0; i < paragraphs.length; i++) {
            if (i > 0) html.append("<br><br>");
            String para = paragraphs[i].trim();
            if (para.isEmpty()) continue;
            para = para.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
            para = para.replace("\n", "<br>");
            html.append("<p style='margin-bottom:0.5rem'>").append(para).append("</p>");
        }
        return html.toString();
    }
}
