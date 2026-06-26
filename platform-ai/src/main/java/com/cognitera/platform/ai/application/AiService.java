package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.*;
import com.cognitera.platform.ai.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The main AI orchestration service that coordinates the full pipeline: retrieval, prompting, inference,
 * temporal and claim validation, grounding, and audit publishing.
 */
@Service
public class AiService implements AiOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final RetrievalAugmentationService retrievalAugmentationService;
    private final ContextAssembler contextAssembler;
    private final PromptBuilder promptBuilder;
    private final ModelProvider modelProvider;
    private final ChatCompletionProvider chatCompletionProvider;
    private final GroundingService groundingService;
    private final AiAuditPublisher auditPublisher;
    private final TemporalConsistencyValidator temporalValidator;
    private final ClaimValidator claimValidator;
    private final StructuredAnswerAssembler answerAssembler;
    private final QueryIntentClassifier intentClassifier;
    private final IndexInspectionService indexInspectionService;

    public AiService(
            RetrievalAugmentationService retrievalAugmentationService,
            ContextAssembler contextAssembler,
            PromptBuilder promptBuilder,
            ModelProvider modelProvider,
            ChatCompletionProvider chatCompletionProvider,
            GroundingService groundingService,
            AiAuditPublisher auditPublisher,
            TemporalConsistencyValidator temporalValidator,
            ClaimValidator claimValidator,
            StructuredAnswerAssembler answerAssembler,
            QueryIntentClassifier intentClassifier,
            IndexInspectionService indexInspectionService
    ) {
        this.retrievalAugmentationService = retrievalAugmentationService;
        this.contextAssembler = contextAssembler;
        this.promptBuilder = promptBuilder;
        this.modelProvider = modelProvider;
        this.chatCompletionProvider = chatCompletionProvider;
        this.groundingService = groundingService;
        this.auditPublisher = auditPublisher;
        this.temporalValidator = temporalValidator;
        this.claimValidator = claimValidator;
        this.answerAssembler = answerAssembler;
        this.intentClassifier = intentClassifier;
        this.indexInspectionService = indexInspectionService;
    }

    @Override
    @Transactional
    public AiResponse answer(AiRequest request) {
        AiRequest normalized = normalize(request);

        // Intent gate: route INDEX_INSPECTION and CORPUS_DISCOVERY queries
        QueryIntent intent = intentClassifier.classify(normalized.question());
        if (intent == QueryIntent.INDEX_INSPECTION || intent == QueryIntent.CORPUS_DISCOVERY) {
            log.info("Routing query to IndexInspectionService: intent={}, query={}",
                    intent, normalized.question());
            return indexInspectionService.inspect(normalized);
        }

        Instant requestedAt = Instant.now();
        RetrievalContext retrievalContext = retrievalAugmentationService.retrieve(normalized);
        PromptContext promptContext = contextAssembler.assemble(normalized, retrievalContext);
        String prompt = promptBuilder.build(promptContext);
        ModelCapabilities capabilities = modelProvider.capabilities(normalized.model());

        auditPublisher.emit(
                normalized.context().actorId(),
                normalized.context().tenantId(),
                "PROMPT_EXECUTED",
                normalized.context().requestId(),
                Map.of(
                        "templateVersion", promptBuilder.templateVersion(),
                        "retrievalSourceCount", Integer.toString(retrievalContext.sources().size())));

        String rawAnswer = chatCompletionProvider.complete(prompt, capabilities);

        TemporalConsistencyValidator.TemporalCheckResult temporalCheck =
                temporalValidator.validate(normalized.question(), rawAnswer);
        if (!temporalCheck.consistent()) {
            log.warn("Temporal inconsistency detected: {}", temporalCheck.issues());
            StringBuilder correctedAnswer = new StringBuilder(rawAnswer);
            correctedAnswer.append("\n\n[Temporal validation warning: ");
            correctedAnswer.append(String.join("; ", temporalCheck.issues()));
            correctedAnswer.append("]");
            rawAnswer = correctedAnswer.toString();
        }

        // Claim validation
        var claimCheck = claimValidator.validate(rawAnswer,
                retrievalContext.authorityReferences());
        if (!claimCheck.valid()) {
            log.warn("Reference-claim validation: {}", claimCheck.unsupportedClaims());
            StringBuilder corrected = new StringBuilder(rawAnswer);
            corrected.append("\n\n[Reference validation: ");
            corrected.append(String.join("; ", claimCheck.unsupportedClaims()));
            corrected.append("]");
            rawAnswer = corrected.toString();
        }

        ReasonedAnswer reasonedAnswer = groundingService.ground(rawAnswer, retrievalContext);
        Instant completedAt = Instant.now();

        InferenceMetadata metadata = new InferenceMetadata(
                capabilities.provider(),
                capabilities.model(),
                requestedAt,
                completedAt,
                normalized.context().correlationId(),
                normalized.context().requestId(),
                promptBuilder.templateVersion(),
                retrievalContext.retrievalStrategy(),
                retrievalContext.sources().stream().map(s -> s.chunkId().toString()).toList(),
                reasonedAnswer.confidence().overallConfidence());

        auditPublisher.emit(
                normalized.context().actorId(),
                normalized.context().tenantId(),
                "MODEL_INFERENCE",
                normalized.context().requestId(),
                Map.of(
                        "provider", capabilities.provider(),
                        "model", capabilities.model(),
                        "grounded", Boolean.toString(reasonedAnswer.grounded()),
                        "citationCount", Integer.toString(reasonedAnswer.sourceCitations().size())));

        return new AiResponse(reasonedAnswer, metadata);
    }

    private AiRequest normalize(AiRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AI request is required");
        }
        if (request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("Question is required");
        }
        AiConversationContext context = request.context() == null
                ? new AiConversationContext(List.of(), null, null, null, null)
                : new AiConversationContext(
                        request.context().messages(),
                        request.context().actorId(),
                        request.context().tenantId(),
                        hasText(request.context().correlationId()) ? request.context().correlationId() : null,
                        hasText(request.context().requestId()) ? request.context().requestId() : null);
        return new AiRequest(
                request.question().trim(),
                request.model(),
                request.searchFilter(),
                context,
                request.maxRetrievalResults() <= 0 ? 5 : Math.min(request.maxRetrievalResults(), 20),
                request.retrievalScope(),
                request.workspaceId());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
