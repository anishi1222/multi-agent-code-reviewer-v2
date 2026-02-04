package dev.logicojp.reviewer.agent;

import java.util.List;

/**
 * Configuration model for a review agent.
 * Loaded from YAML files in the agents/ directory.
 */
public class AgentConfig {
    
    private String name;
    private String displayName;
    private String model;
    private String systemPrompt;
    private String reviewPrompt;
    private String outputFormat;
    private List<String> focusAreas;
    
    public AgentConfig() {
        // Default constructor for YAML deserialization
    }
    
    public AgentConfig(String name, String displayName, String model, 
                       String systemPrompt, List<String> focusAreas) {
        this.name = name;
        this.displayName = displayName;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.focusAreas = focusAreas;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getReviewPrompt() {
        return reviewPrompt;
    }

    public void setReviewPrompt(String reviewPrompt) {
        this.reviewPrompt = reviewPrompt;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    public List<String> getFocusAreas() {
        return focusAreas;
    }
    
    public void setFocusAreas(List<String> focusAreas) {
        this.focusAreas = focusAreas;
    }
    
    /**
     * Builds the complete system prompt including output format instructions.
     */
    public String buildFullSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            sb.append(systemPrompt.trim()).append("\n\n");
        }

        if (focusAreas != null && !focusAreas.isEmpty()) {
            sb.append("## レビュー観点\n");
            for (String area : focusAreas) {
                sb.append("- ").append(area).append("\n");
            }
            sb.append("\n");
        }

        if (outputFormat != null && !outputFormat.isBlank()) {
            String outputText = outputFormat.trim();
            if (!outputText.startsWith("##")) {
                sb.append("## 出力フォーマット\n\n");
            }
            sb.append(outputText).append("\n");
        }

        return sb.toString();
    }

    public String buildReviewPrompt(String repository) {
        if (reviewPrompt == null || reviewPrompt.isBlank()) {
            throw new IllegalStateException("Review prompt is not configured for agent: " + name);
        }

        String focusAreaText = formatFocusAreas();
        return reviewPrompt
            .replace("${repository}", repository)
            .replace("${displayName}", displayName != null ? displayName : name)
            .replace("${name}", name)
            .replace("${focusAreas}", focusAreaText);
    }

    private String formatFocusAreas() {
        if (focusAreas == null || focusAreas.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String area : focusAreas) {
            sb.append("- ").append(area).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "AgentConfig{name='" + name + "', displayName='" + displayName + "'}";
    }
}
