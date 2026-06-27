package com.cognitera.platform.web;

import com.cognitera.platform.ai.api.AiFacade;
import com.cognitera.platform.ai.model.AiRequest;
import com.cognitera.platform.ai.model.AiResponse;
import com.cognitera.platform.ai.model.RetrievalScope;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Thymeleaf page controller for the AI retrieval-augmented query page.
 * Executes the full AI pipeline: retrieval → prompt construction → LLM inference →
 * grounding → validation → structured answer.
 */
@Controller
public class AiPageController {

    private final ObjectProvider<AiFacade> aiFacadeProvider;

    public AiPageController(ObjectProvider<AiFacade> aiFacadeProvider) {
        this.aiFacadeProvider = aiFacadeProvider;
    }

    @GetMapping("/ai")
    public String ai(Model model) {
        model.addAttribute("model", "");
        model.addAttribute("scope", "ALL_DOCUMENTS");
        model.addAttribute("question", "");
        model.addAttribute("formattedAnswer", null);
        return "ai/index";
    }

    @PostMapping("/ai")
    public String handleAi(@RequestParam(required = false) String modelParam,
                           @RequestParam(required = false) String scope,
                           @RequestParam(required = false) String workspaceId,
                           @RequestParam String question,
                           Model pageModel) {
        pageModel.addAttribute("model", modelParam != null ? modelParam : "");
        pageModel.addAttribute("scope", scope);
        pageModel.addAttribute("question", question);

        AiFacade aiFacade = aiFacadeProvider.getIfAvailable();
        if (aiFacade == null) {
            pageModel.addAttribute("formattedAnswer",
                    "<div class=\"alert alert-warning\">AI service is not configured. "
                    + "Ensure the embedding/Ollama configuration is active.</div>");
            return "ai/index";
        }

        try {
            UUID wsId = workspaceId != null && !workspaceId.isBlank()
                    ? UUID.fromString(workspaceId) : null;
            AiRequest request = new AiRequest(question, modelParam, null, null,
                    20, RetrievalScope.ALL_DOCUMENTS, wsId);
            AiResponse response = aiFacade.answer(request);

            StringBuilder html = new StringBuilder();

            // Answer section
            String answerText = response.answer() != null ? response.answer().answer() : "";
            java.util.List<com.cognitera.platform.ai.model.SourceCitation> sources =
                    response.answer() != null ? response.answer().sourceCitations() : java.util.List.of();
            String strategy = response.metadata() != null ? response.metadata().retrievalStrategy() : "hybrid";
            double confidence = response.answer() != null && response.answer().confidence() != null
                    ? response.answer().confidence().overallConfidence() : 0.0;

            html.append("<div class=\"card mb-3\"><div class=\"card-body\">");
            html.append("<h5>Answer ")
                .append("<small class=\"text-muted\">(confidence: ")
                .append(String.format("%.0f%%", confidence * 100)).append(")</small></h5>");
            html.append("<div class=\"mb-0\">")
                .append(escapeHtml(answerText))
                .append("</div>");
            html.append("</div></div>");

            // Sources section
            html.append("<div class=\"card mb-3\"><div class=\"card-body\">");
            html.append("<h5>Retrieved Sources</h5>");
            if (!sources.isEmpty()) {
                html.append("<p class=\"text-muted\">")
                    .append("Strategy: ").append(escapeHtml(strategy))
                    .append(" | Sources: ").append(sources.size())
                    .append("</p>");
                html.append("<ul class=\"list-group list-group-flush\">");
                sources.stream().limit(10).forEach(s -> {
                    html.append("<li class=\"list-group-item\">");
                    html.append("<strong>").append(escapeHtml(s.title())).append("</strong>");
                    html.append("<br><small class=\"text-muted\">")
                        .append(escapeHtml(s.excerpt() != null ? s.excerpt() : ""))
                        .append("</small>");
                    html.append("</li>");
                });
                html.append("</ul>");
            }
            html.append("</div></div>");

            pageModel.addAttribute("formattedAnswer", html.toString());
        } catch (Exception e) {
            pageModel.addAttribute("formattedAnswer",
                    "<div class=\"alert alert-danger\">AI query failed: "
                    + escapeHtml(e.getMessage()) + "</div>");
        }
        return "ai/index";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
