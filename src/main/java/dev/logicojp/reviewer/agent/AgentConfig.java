package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.ModelConfig;
import dev.logicojp.reviewer.skill.SkillDefinition;

import java.util.List;

/// Configuration model for a review agent.
/// Loaded from YAML files in the agents/ directory.
///
/// This record is a pure data carrier. Prompt construction logic is
/// in {@link AgentPromptBuilder}.
public record AgentConfig(
    String name,
    String displayName,
    String model,
    String systemPrompt,
    String instruction,
    String outputFormat,
    List<String> focusAreas,
    List<SkillDefinition> skills
) {

    public AgentConfig {
        name = name == null ? "" : name;
        displayName = (displayName == null || displayName.isBlank()) ? name : displayName;
        model = (model == null || model.isBlank()) ? ModelConfig.DEFAULT_MODEL : model;
        outputFormat = normalizeOutputFormat(outputFormat);
        focusAreas = focusAreas == null ? List.of() : List.copyOf(focusAreas);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public AgentConfig withModel(String overrideModel) {
        return new AgentConfig(
            this.name, this.displayName, overrideModel, this.systemPrompt,
            this.instruction, this.outputFormat, this.focusAreas, this.skills
        );
    }

    public AgentConfig withSkills(List<SkillDefinition> newSkills) {
        return new AgentConfig(
            this.name, this.displayName, this.model, this.systemPrompt,
            this.instruction, this.outputFormat, this.focusAreas, newSkills
        );
    }

    /// Validates required fields. Delegates to {@link AgentConfigValidator}.
    public void validateRequired() {
        AgentConfigValidator.validateRequired(this);
    }

    private static String normalizeOutputFormat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("##")) {
            return trimmed;
        }
        return "## Output Format\n\n" + trimmed;
    }

    /// Builds the complete system prompt including output format instructions.
    /// Delegates to {@link AgentPromptBuilder}.
    public String buildFullSystemPrompt() {
        return AgentPromptBuilder.buildFullSystemPrompt(this);
    }

    /// Builds the instruction for a GitHub repository review.
    /// Delegates to {@link AgentPromptBuilder}.
    public String buildInstruction(String repository) {
        return AgentPromptBuilder.buildInstruction(this, repository);
    }

    /// Builds the instruction for a local directory review.
    /// Delegates to {@link AgentPromptBuilder}.
    public String buildLocalInstruction(String targetName, String sourceContent) {
        return AgentPromptBuilder.buildLocalInstruction(this, targetName, sourceContent);
    }

    /// Builds the local instruction without embedding source content.
    public String buildLocalInstructionBase(String targetName) {
        return AgentPromptBuilder.buildLocalInstructionBase(this, targetName);
    }

    @Override
    public String toString() {
        return "AgentConfig{name='" + name + "', displayName='" + displayName + "'}";
    }
}
