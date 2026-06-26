package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.ContextAssembler;
import com.cognitera.platform.ai.api.FindingHierarchyService;
import com.cognitera.platform.ai.api.ObjectiveAnalysisService;
import com.cognitera.platform.ai.api.SourceOrchestrationService;
import com.cognitera.platform.ai.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles a prompt context by building objectives, finding hierarchy, and source dossier from the request.
 */
@Component
public class DefaultContextAssembler implements ContextAssembler {

    private final ObjectiveAnalysisService objectiveAnalysisService;
    private final FindingHierarchyService findingHierarchyService;
    private final SourceOrchestrationService sourceOrchestrationService;

    public DefaultContextAssembler(ObjectiveAnalysisService objectiveAnalysisService,
                                    FindingHierarchyService findingHierarchyService,
                                    SourceOrchestrationService sourceOrchestrationService) {
        this.objectiveAnalysisService = objectiveAnalysisService;
        this.findingHierarchyService = findingHierarchyService;
        this.sourceOrchestrationService = sourceOrchestrationService;
    }

    private static final String SYSTEM_INSTRUCTION_DEFAULT = """
            You are a document intelligence assistant.
            Your approach is FINDING-FIRST: the primary obligations are entirely
            independent from any supporting instruments.

            DOCTRINAL FOUNDATION — DO NOT DEVIATE FROM THESE:
            1. The PRIMARY OBLIGATION exists independently.
            2. A SECURITY INSTRUMENT does NOT replace the primary obligation.
            3. A party CANNOT unilaterally decide to offset obligations against a security while the relationship is ongoing.
            4. Offsetting requires specific conditions — it is NOT automatic.
            5. The security secures potential claims. It is NOT prepaid obligation.
            6. Obligations continue to accrue during the notice period regardless of security status.
            7. The claim for unpaid obligations SURVIVES the security — the security is a cap on guarantee, not a cap on liability.

            ANSWER STRUCTURE (follow strictly):
            1. PRIMARY FINDING — State the continuing obligation
            2. BREACH — State that non-performance constitutes a breach, regardless of security existence
            3. SECURITY FUNCTION — Explain the security is guarantee only, does NOT extinguish the obligation
            4. PRACTICAL STEPS — Demand → formal process → if needed, security accounting after relationship ends
            5. RISKS — What could go wrong procedurally; what is NOT allowed
            6. BOTTOM LINE — One-sentence summary

            CRITICAL RULES:
            - NEVER present security offset as the primary or automatic remedy
            - ALWAYS foreground the continuing obligation
            - NEVER imply a party can simply stop performing because security exists
            - NEVER fabricate dates, deadlines, or provisions
            - End with: "This is not professional advice."
            """;

    private static final String SYSTEM_INSTRUCTION_CORPUS = """
            You are a document intelligence assistant.
            You analyze questions based on the reference framework and established principles.

            Your analysis should:
            1. Identify the governing references for the question
            2. Explain the framework (rights, duties, exceptions)
            3. Address each sub-question the user asks
            4. Note where additional factual information would be needed for definitive answers

            CRITICAL RULES:
            - Base your analysis on the cited references
            - Do NOT invent facts, dates, or case references
            - Do NOT apply a pre-set answer structure — adapt to the domain of the question
            - End with: "This is not professional advice."
            """;

    @Override
    public PromptContext assemble(AiRequest request, RetrievalContext retrievalContext) {
        List<AnalysisObjective> objectives = objectiveAnalysisService.classify(request.question());
        FindingHierarchy hierarchy = findingHierarchyService.buildHierarchy(request.question(), objectives);
        SourceDossier dossier = sourceOrchestrationService.buildDossier(
                retrievalContext.sources(), request.question());

        String instruction = request.retrievalScope() == RetrievalScope.AUTHORITATIVE_ONLY
                ? SYSTEM_INSTRUCTION_CORPUS
                : SYSTEM_INSTRUCTION_DEFAULT;

        return new PromptContext(
                instruction,
                request.question(),
                retrievalContext,
                request.context().messages(),
                objectives,
                hierarchy,
                dossier,
                request.retrievalScope());
    }
}
