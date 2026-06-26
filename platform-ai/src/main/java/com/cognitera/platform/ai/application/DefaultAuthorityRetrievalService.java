package com.cognitera.platform.ai.application;

import com.cognitera.platform.ai.api.AuthorityRetrievalService;
import com.cognitera.platform.ai.model.AuthorityReference;
import com.cognitera.platform.ai.model.ExtractedConcept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Retrieves authority references by building match results from extracted concepts and governing references.
 */
@Service
public class DefaultAuthorityRetrievalService implements AuthorityRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuthorityRetrievalService.class);
    private static final UUID BGB_DOC_ID = UUID.nameUUIDFromBytes("BGB_REFERENCE_DATA".getBytes());

    @Override
    public List<AuthorityReference> retrieveAuthorities(String query, List<ExtractedConcept> concepts) {
        Set<String> entryNumbers = new LinkedHashSet<>();
        Map<String, String> basis = new LinkedHashMap<>();
        Map<String, Double> conceptConfidence = new LinkedHashMap<>();

        for (ExtractedConcept concept : concepts) {
            for (String entry : concept.governingReferences()) {
                entryNumbers.add(entry);
                basis.merge(entry, concept.label(), (a, b) -> a + "; " + b);
                conceptConfidence.merge(entry, concept.confidence(), Math::max);
            }
        }

        List<AuthorityReference> references = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String entryNum : entryNumbers) {
            if (!seen.add(entryNum)) continue;

            UUID chunkId = UUID.nameUUIDFromBytes(
                    (entryNum + "_chunk_0").getBytes());

            double score = conceptConfidence.getOrDefault(entryNum, 0.5);
            String basisText = basis.getOrDefault(entryNum, "General");

            AuthorityReference.ReferenceTier tier;
            if (score >= 0.7) {
                tier = AuthorityReference.ReferenceTier.PRIMARY;
            } else if (score >= 0.4) {
                tier = AuthorityReference.ReferenceTier.SUPPORTING;
            } else {
                tier = AuthorityReference.ReferenceTier.BACKGROUND;
            }

            references.add(new AuthorityReference(
                    BGB_DOC_ID,
                    chunkId,
                    "Ref § " + entryNum,
                    "DOMAIN",
                    entryNum,
                    "Entry " + entryNum,
                    "Reference text for entry " + entryNum,
                    "general",
                    basisText,
                    score,
                    score,
                    tier));
        }

        references.sort(Comparator.comparingDouble(AuthorityReference::relevanceScore).reversed());
        log.debug("Authority retrieval: {} references for {} concepts", references.size(), concepts.size());
        return references;
    }
}
