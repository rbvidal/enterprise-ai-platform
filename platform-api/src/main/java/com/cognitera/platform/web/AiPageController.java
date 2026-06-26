package com.cognitera.platform.web;

import com.cognitera.platform.ai.api.RetrievalAugmentationService;
import com.cognitera.platform.ai.model.AiRequest;
import com.cognitera.platform.ai.model.RetrievalContext;
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
 */
@Controller
public class AiPageController {

    private final ObjectProvider<RetrievalAugmentationService> aiServiceProvider;

    /**
     * Constructs the controller with an optional AI service provider.
     */
    public AiPageController(ObjectProvider<RetrievalAugmentationService> aiServiceProvider) {
        this.aiServiceProvider = aiServiceProvider;
    }

    /**
     * Renders the AI query page with empty form state.
     */
    @GetMapping("/ai")
    public String ai(Model model) {
        model.addAttribute("model", "");
        model.addAttribute("scope", "ALL_DOCUMENTS");
        model.addAttribute("question", "");
        model.addAttribute("formattedAnswer", null);
        return "ai/index";
    }

    /**
     * Processes the AI query, retrieves context, and renders formatted results as HTML.
     */
    @PostMapping("/ai")
    public String handleAi(@RequestParam(required = false) String modelParam,
                           @RequestParam(defaultValue = "HYBRID") String scope,
                           @RequestParam(required = false) String workspaceId,
                           @RequestParam String question,
                           Model pageModel) {
        pageModel.addAttribute("model", modelParam != null ? modelParam : "");
        pageModel.addAttribute("scope", scope);
        pageModel.addAttribute("question", question);

        RetrievalAugmentationService aiService = aiServiceProvider.getIfAvailable();
        if (aiService == null) {
            pageModel.addAttribute("formattedAnswer",
                    "<div class=\"alert alert-warning\">AI service is not configured. "
                    + "Ensure the embedding/Ollama configuration is active.</div>");
            return "ai/index";
        }

        try {
            RetrievalScope retrievalScope = RetrievalScope.ALL_DOCUMENTS;
            UUID wsId = workspaceId != null && !workspaceId.isBlank()
                    ? UUID.fromString(workspaceId) : null;
            AiRequest request = new AiRequest(question, modelParam, null, null,
                    20, retrievalScope, wsId);
            RetrievalContext context = aiService.retrieve(request);

            StringBuilder html = new StringBuilder();
            html.append("<div class=\"card mb-3\"><div class=\"card-body\">");
            html.append("<h5>Retrieval Results</h5>");
            html.append("<p class=\"text-muted\">Strategy: ").append(context.retrievalStrategy())
                .append(" | Sources found: ").append(context.sources().size()).append("</p>");
            if (!context.sources().isEmpty()) {
                html.append("<ul class=\"list-group list-group-flush\">");
                context.sources().stream().limit(10).forEach(s -> {
                    html.append("<li class=\"list-group-item\">");
                    html.append("<strong>").append(escapeHtml(s.title())).append("</strong>");
                    html.append(" <span class=\"badge bg-secondary\">")
                        .append(escapeHtml(s.tier().name())).append("</span>");
                    html.append("<br><small class=\"text-muted\">")
                        .append(escapeHtml(s.excerpt() != null ? s.excerpt() : "")).append("</small>");
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
