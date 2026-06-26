package com.cognitera.platform.ai.api;

import com.cognitera.platform.ai.model.AnalysisObjective;
import com.cognitera.platform.ai.model.FindingHierarchy;

import java.util.List;

/**
 * Builds a finding hierarchy from a query and analysis objectives.
 */
public interface FindingHierarchyService {
    /**
     * Builds a {@link FindingHierarchy} categorizing findings by role based on the given objectives.
     */
    FindingHierarchy buildHierarchy(String query, List<AnalysisObjective> objectives);
}
