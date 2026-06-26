package com.cognitera.platform.api.config;

import com.cognitera.platform.document.api.DocumentFacade;
import com.cognitera.platform.document.api.TextExtractionService;
import com.cognitera.platform.search.api.*;
import com.cognitera.platform.search.application.*;
import com.cognitera.platform.search.application.ollama.OllamaEmbeddingConfig;
import com.cognitera.platform.search.application.ollama.OllamaEmbeddingProvider;
import com.cognitera.platform.search.application.ollama.OllamaMetadataExtractionService;
import com.cognitera.platform.search.application.ollama.OllamaRerankingProvider;
import com.cognitera.platform.search.application.qdrant.QdrantCollectionManager;
import com.cognitera.platform.search.application.qdrant.QdrantProperties;
import com.cognitera.platform.search.application.qdrant.QdrantVectorSearchProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires up search infrastructure beans (embedding, vector search,
 * chunking, indexing orchestration) based on property-driven conditional activation.
 */
@Configuration
@EnableConfigurationProperties({QdrantProperties.class, OllamaEmbeddingConfig.class})
public class SearchInfrastructureConfig {

    /**
     * Provides the default vector search provider, falling back to a no-op implementation when Qdrant is unavailable.
     */
    @Bean
    public VectorSearchProvider vectorSearchProvider(
            @org.springframework.beans.factory.annotation.Autowired(required = false) QdrantVectorSearchProvider qdrant) {
        if (qdrant != null) return qdrant;
        return new com.cognitera.platform.search.application.NoOpVectorSearchProvider();
    }

    /**
     * Creates the Ollama-based embedding provider when the {@code platform.search.embedding.ollama.base-url} property is set.
     */
    @Bean
    @ConditionalOnProperty(name = "platform.search.embedding.ollama.base-url")
    public EmbeddingProvider embeddingProvider(OllamaEmbeddingConfig config) {
        return new OllamaEmbeddingProvider(config);
    }

    /**
     * Creates the Ollama-based metadata extraction service when the embedding base URL property is set.
     */
    @Bean
    @ConditionalOnProperty(name = "platform.search.embedding.ollama.base-url")
    public MetadataExtractionService metadataExtractionService(OllamaEmbeddingConfig config) {
        return new OllamaMetadataExtractionService(config.baseUrl(), config.chatModel());
    }

    /**
     * Creates the Ollama-based reranking provider when the embedding base URL property is set.
     */
    @Bean
    @ConditionalOnProperty(name = "platform.search.embedding.ollama.base-url")
    public RerankingProvider rerankingProvider(OllamaEmbeddingConfig config) {
        return new OllamaRerankingProvider(config.baseUrl(), config.chatModel());
    }

    /**
     * Creates the Qdrant collection manager and ensures the collection exists when the Qdrant host property is set.
     */
    @Bean
    @ConditionalOnProperty(name = "platform.search.qdrant.host")
    public QdrantCollectionManager qdrantCollectionManager(QdrantProperties properties) {
        QdrantCollectionManager manager = new QdrantCollectionManager(properties);
        manager.ensureCollectionExists();
        return manager;
    }

    /**
     * Creates the Qdrant-based vector search provider when the Qdrant host property is set.
     */
    @Bean
    @ConditionalOnProperty(name = "platform.search.qdrant.host")
    public QdrantVectorSearchProvider qdrantVectorSearchProvider(
            QdrantProperties properties,
            EmbeddingProvider embeddingProvider) {
        return new QdrantVectorSearchProvider(properties, embeddingProvider);
    }

    /**
     * Provides the default sentence-aware chunking strategy.
     */
    @Bean
    public ChunkingStrategy chunkingStrategy() {
        return new SentenceAwareChunkingStrategy();
    }

    /**
     * Creates the indexing orchestration service when an embedding provider is available.
     */
    @Bean
    @ConditionalOnBean(EmbeddingProvider.class)
    public IndexingOrchestrationService indexingOrchestrationService(
            DocumentFacade documents,
            TextExtractionService textExtractionService,
            ChunkingStrategy chunkingStrategy,
            ChunkManagementService chunkManagementService,
            EmbeddingProvider embeddingProvider,
            VectorSearchProvider vectorSearchProvider) {
        return new DefaultIndexingOrchestrationService(
                documents, textExtractionService, chunkingStrategy,
                chunkManagementService, embeddingProvider, vectorSearchProvider);
    }

    /**
     * Provides the default keyword-based query intent classifier.
     */
    @Bean
    public QueryIntentClassifier queryIntentClassifier() {
        return new KeywordIntentClassifier();
    }

    /**
     * Creates the document lifecycle hook that syncs index state when an indexing orchestration service is available.
     */
    @Bean
    @ConditionalOnBean(IndexingOrchestrationService.class)
    public DocumentLifecycleHook documentLifecycleHook(
            com.cognitera.platform.search.infrastructure.persistence.JpaDocumentChunkRepository chunks,
            VectorSearchProvider vectorSearchProvider,
            IndexingOrchestrationService indexingOrchestrationService) {
        return new com.cognitera.platform.search.application.DefaultDocumentLifecycleHook(
                chunks, vectorSearchProvider, indexingOrchestrationService);
    }
}
