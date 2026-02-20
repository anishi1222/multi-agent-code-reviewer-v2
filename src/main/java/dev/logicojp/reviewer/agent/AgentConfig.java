package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.ModelConfig;
import dev.logicojp.reviewer.skill.SkillDefinition;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

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

    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);

    // --- Validation constants (inlined from v1 AgentConfigValidator) ---

    private static final List<String> REQUIRED_OUTPUT_SECTIONS = List.of(
        "Priority", "指摘の概要", "推奨対応", "効果"
    );

    private static final Pattern PRIORITY_PATTERN = Pattern.compile(
        "Critical|High|Medium|Low", Pattern.CASE_INSENSITIVE);

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

        public Builder name(String name) { this.name = name; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder systemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; return this; }
        public Builder instruction(String instruction) { this.instruction = instruction; return this; }
        public Builder outputFormat(String outputFormat) { this.outputFormat = outputFormat; return this; }
        public Builder focusAreas(List<String> focusAreas) { this.focusAreas = focusAreas; return this; }
        public Builder skills(List<SkillDefinition> skills) { this.skills = skills; return this; }

        public AgentConfig build() {
            return new AgentConfig(name, displayName, model, systemPrompt,
                instruction, outputFormat, focusAreas, skills);
        }
    }

    /// Validates required fields. Inlined from v1 AgentConfigValidator.
    public void validateRequired() {
        var missing = new StringJoiner(", ");
        if (name == null || name.isBlank()) {
            missing.add("name");
        }
        if (systemPrompt == null || systemPrompt.isBlank()) {
            missing.add("systemPrompt");
        }
        if (instruction == null || instruction.isBlank()) {
            missing.add("instruction");
        }
        if (missing.length() > 0) {
            throw new IllegalArgumentException("Missing required agent fields: " + missing);
        }
        if (focusAreas.isEmpty()) {
            logger.warn("Agent '{}' has no focusAreas; proceeding with defaults.", name);
        }
        validateOutputFormat();
    }

    private void validateOutputFormat() {
        if (outputFormat == null || outputFormat.isBlank()) {
            return;
        }
        List<String> missingSections = new ArrayList<>();
        for (String section : REQUIRED_OUTPUT_SECTIONS) {
            if (!outputFormat.contains(section)) {
                missingSections.add(section);
            }
        }
        if (!missingSections.isEmpty()) {
            logger.warn("Agent '{}' outputFormat is missing recommended sections: {}",
                name, String.join(", ", missingSections));
        }
        if (!PRIORITY_PATTERN.matcher(outputFormat).find()) {
            logger.warn("Agent '{}' outputFormat does not contain Priority levels "
                + "(Critical/High/Medium/Low).", name);
        }
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
