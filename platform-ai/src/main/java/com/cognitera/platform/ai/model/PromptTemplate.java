package com.cognitera.platform.ai.model;

import java.util.List;
import java.util.Map;

/**
 * A versioned, identifiable prompt template.
 * Every prompt used in the platform is registered here for reproducibility and auditing.
 */
public class PromptTemplate {

    private String id;
    private int version;
    private String description;
    private String template;
    private List<String> variables;
    private String expectedOutputType;
    private List<String> supportedModels;
    private Map<String, String> metadata;

    public PromptTemplate() {}

    public PromptTemplate(String id, int version, String description, String template,
                          List<String> variables, String expectedOutputType,
                          List<String> supportedModels, Map<String, String> metadata) {
        this.id = id;
        this.version = version;
        this.description = description;
        this.template = template;
        this.variables = variables != null ? List.copyOf(variables) : List.of();
        this.expectedOutputType = expectedOutputType;
        this.supportedModels = supportedModels != null ? List.copyOf(supportedModels) : List.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public List<String> getVariables() { return variables; }
    public void setVariables(List<String> variables) { this.variables = variables; }
    public String getExpectedOutputType() { return expectedOutputType; }
    public void setExpectedOutputType(String expectedOutputType) { this.expectedOutputType = expectedOutputType; }
    public List<String> getSupportedModels() { return supportedModels; }
    public void setSupportedModels(List<String> supportedModels) { this.supportedModels = supportedModels; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    /** Returns the fully qualified prompt identifier: "id/v{version}". */
    public String getQualifiedId() { return id + "/v" + version; }

    /** Renders the template by substituting {{variable}} placeholders. */
    public String render(Map<String, String> values) {
        String result = template;
        for (var entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
