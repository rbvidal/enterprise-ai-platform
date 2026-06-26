package com.cognitera.platform.api.web;

import com.cognitera.platform.api.dto.search.SearchRequest;
import com.cognitera.platform.api.dto.search.SearchResultPageResponse;
import com.cognitera.platform.search.api.SearchFacade;
import com.cognitera.platform.search.model.MetadataFilter;
import com.cognitera.platform.search.model.SearchFilter;
import com.cognitera.platform.search.model.SearchQuery;
import com.cognitera.platform.search.model.SearchRequestContext;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for performing full-text and semantic document searches.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchFacade search;

    /**
     * Constructs the controller with the search facade.
     */
    public SearchController(SearchFacade search) {
        this.search = search;
    }

    /**
     * Executes a search query with optional filtering and returns paginated results.
     */
    @PostMapping
    public SearchResultPageResponse search(@Valid @RequestBody SearchRequest request,
                                            Authentication authentication) {
        return SearchResultPageResponse.from(search.search(new SearchQuery(
                request.query(),
                request.mode(),
                new SearchFilter(
                        request.documentIds(),
                        request.documentType(),
                        request.category(),
                        request.tag(),
                        request.source(),
                        request.tenantId(),
                        request.createdFrom(),
                        request.createdTo(),
                        metadata(request)),
                new SearchRequestContext(authentication.getName(), request.tenantId(), null, null),
                request.page() == null ? 0 : request.page(),
                request.size() == null ? 20 : request.size())));
    }

    private List<MetadataFilter> metadata(SearchRequest request) {
        if (request.metadata() == null) {
            return List.of();
        }
        return request.metadata().stream()
                .map(filter -> new MetadataFilter(filter.key(), filter.value()))
                .toList();
    }
}
