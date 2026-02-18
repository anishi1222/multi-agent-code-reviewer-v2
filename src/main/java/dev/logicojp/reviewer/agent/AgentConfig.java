package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.ModelConfig;
import dev.logicojp.reviewer.skill.SkillDefinition;
import io.micronaut.core.annotation.Nullable;

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
    @Nullable String systemPrompt,
    @Nullable String instruction,
    @Nullable String outputFormat,
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
        return Builder.from(this)
            .model(overrideModel)
            .build();
    }

    public AgentConfig withSkills(List<SkillDefinition> newSkills) {
        return Builder.from(this)
            .skills(newSkills)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String displayName;
        private String model;
        private String systemPrompt;
        private String instruction;
        private String outputFormat;
        private List<String> focusAreas;
        private List<SkillDefinition> skills;

        private Builder() {
        }

        public static Builder from(AgentConfig source) {
            return new Builder()
                .name(source.name)
                .displayName(source.displayName)
                .model(source.model)
                .systemPrompt(source.systemPrompt)
                .instruction(source.instruction)
                .outputFormat(source.outputFormat)
                .focusAreas(source.focusAreas)
                .skills(source.skills);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder instruction(String instruction) {
            this.instruction = instruction;
            return this;
        }

        public Builder outputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        public Builder focusAreas(List<String> focusAreas) {
            this.focusAreas = focusAreas;
            return this;
        }

        public Builder skills(List<SkillDefinition> skills) {
            this.skills = skills;
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(name, displayName, model, systemPrompt, instruction, outputFormat, focusAreas, skills);
        }
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

    @Override
    public String toString() {
        return "AgentConfig{name='" + name + "', displayName='" + displayName + "'}";
    }
}
