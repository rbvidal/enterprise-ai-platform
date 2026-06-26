package com.cognitera.platform.web;

import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.search.api.ChunkManagementService;
import com.cognitera.platform.search.model.SearchFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;
import java.util.UUID;

/**
 * Thymeleaf page controller for the search and chunk listing pages.
 */
@Controller
public class SearchPageController {

    private final ChunkManagementService chunkManagementService;

    /**
     * Constructs the controller with the chunk management service.
     */
    public SearchPageController(ChunkManagementService chunkManagementService) {
        this.chunkManagementService = chunkManagementService;
    }

    /**
     * Renders the search page with search modes, document types, and an empty search form.
     */
    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("modes", new String[]{"hybrid", "semantic", "keyword"});
        model.addAttribute("types", DocumentType.values());
        model.addAttribute("form", new SearchForm("", "", ""));
        model.addAttribute("results", null);
        return "search/index";
    }

    /**
     * Renders the chunks listing page, optionally filtered by document ID.
     */
    @GetMapping("/chunks")
    public String chunks(@RequestParam(required = false) String documentId,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        if (documentId != null && !documentId.isBlank()) {
            try {
                UUID docUuid = UUID.fromString(documentId.trim());
                var filter = new SearchFilter(Set.of(docUuid), null, null, null, null,
                        null, null, null, java.util.List.of());
                var chunkList = chunkManagementService.findChunks(filter, page, 200);
                model.addAttribute("chunks", chunkList);
                model.addAttribute("hasNext", chunkList.size() >= 200);
            } catch (IllegalArgumentException e) {
                model.addAttribute("chunks", java.util.List.of());
                model.addAttribute("hasNext", false);
                model.addAttribute("error", "Invalid document ID: " + e.getMessage());
            }
        } else {
            model.addAttribute("chunks", java.util.List.of());
            model.addAttribute("hasNext", false);
        }
        model.addAttribute("currentPage", page);
        model.addAttribute("currentDocumentId", documentId != null ? documentId : "");
        return "search/chunks";
    }

    /**
     * Form-backing record for search query parameters.
     */
    public record SearchForm(String query, String category, String tag) {}
}
