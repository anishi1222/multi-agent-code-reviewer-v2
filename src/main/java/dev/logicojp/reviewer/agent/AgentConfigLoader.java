package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.ReviewerConfig;
import dev.logicojp.reviewer.instruction.CustomInstructionSafetyValidator;
import dev.logicojp.reviewer.skill.SkillDefinition;
import dev.logicojp.reviewer.skill.SkillMarkdownParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/// Loads agent configurations from external files.
/// Supports GitHub Copilot agent definition format (.agent.md).
///
/// Agent files can be placed in:
/// - agents/ directory
/// - .github/agents/ directory
///
/// Skills are loaded from the configured skills directory following the Agent Skills spec.
public class AgentConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(AgentConfigLoader.class);

    private final List<Path> agentDirectories;
    private final AgentMarkdownParser markdownParser;
    private final SkillMarkdownParser skillParser;
    private final String skillsDirectory;

    public static Builder builder(List<Path> agentDirectories) {
        return new Builder(agentDirectories);
    }

    public static final class Builder {
        private final List<Path> agentDirectories;
        private ReviewerConfig.Skills skillConfig = ReviewerConfig.Skills.defaults();
        private String defaultOutputFormat;

        private Builder(List<Path> agentDirectories) {
            this.agentDirectories = List.copyOf(agentDirectories);
        }

        public Builder skillConfig(ReviewerConfig.Skills skillConfig) {
            this.skillConfig = skillConfig != null ? skillConfig : ReviewerConfig.Skills.defaults();
            return this;
        }

        public Builder defaultOutputFormat(String defaultOutputFormat) {
            this.defaultOutputFormat = defaultOutputFormat;
            return this;
        }

        public AgentConfigLoader build() {
            return new AgentConfigLoader(agentDirectories, skillConfig, defaultOutputFormat);
        }
    }

    /// Creates a loader with a single agents directory and default skill settings.
    public AgentConfigLoader(Path agentsDirectory) {
        this(List.of(agentsDirectory), ReviewerConfig.Skills.defaults(), null);
    }

    /// Creates a loader with multiple agent directories, skill configuration,
    /// and a default output format loaded from an external template.
    public AgentConfigLoader(List<Path> agentDirectories, ReviewerConfig.Skills skillConfig,
                             String defaultOutputFormat) {
        this.agentDirectories = List.copyOf(agentDirectories);
        this.markdownParser = new AgentMarkdownParser(defaultOutputFormat);
        this.skillParser = new SkillMarkdownParser(skillConfig.filename());
        this.skillsDirectory = skillConfig.directory();
    }

    /// Loads all agent configurations from all configured directories.
    public Map<String, AgentConfig> loadAllAgents() throws IOException {
        return loadAgentsInternal(null);
    }

    /// Loads specific agents by name.
    public Map<String, AgentConfig> loadAgents(List<String> agentNames) throws IOException {
        Map<String, AgentConfig> agents = loadAgentsInternal(new HashSet<>(agentNames));

        for (String name : agentNames) {
            if (!agents.containsKey(name)) {
                logger.warn("Agent not found: {}", name);
            }
        }

        return agents;
    }

    private Map<String, AgentConfig> loadAgentsInternal(Set<String> filter) throws IOException {
        Map<String, AgentConfig> agents = new LinkedHashMap<>();
        List<SkillDefinition> globalSkills = loadGlobalSkills();

        for (Path directory : agentDirectories) {
            if (!Files.exists(directory)) {
                logger.debug("Agents directory does not exist: {}", directory);
                continue;
            }

            logger.info("Loading agents from: {}", directory);

            List<Path> files = listAgentFiles(directory, filter);
            for (Path file : files) {
                try {
                    AgentConfig config = markdownParser.parse(file);
                    if (config != null) {
                        Optional<String> suspiciousField = firstSuspiciousField(config);
                        if (suspiciousField.isPresent()) {
                            logger.warn("Agent file contains suspicious patterns in '{}', skipping: {}",
                                suspiciousField.get(), file);
                            continue;
                        }
                        config = applySkills(config, globalSkills);
                        config.validateRequired();
                        agents.put(config.name(), config);
                        logger.info("Loaded agent: {} from {}", config.name(), file.getFileName());
                    }
                } catch (IOException | IllegalArgumentException | UncheckedIOException e) {
                    logger.error("Failed to load agent from {}: {}", file, e.getMessage(), e);
                }
            }
        }

        if (agents.isEmpty()) {
            logger.warn("No agents found in any configured directory");
        }

        return agents;
    }

    private AgentConfig applySkills(AgentConfig config, List<SkillDefinition> globalSkills) {
        List<SkillDefinition> agentSkills = collectSkillsForAgent(config.name(), globalSkills);
        if (agentSkills.isEmpty()) {
            return config;
        }
        logger.info("Loaded {} skills for agent: {}", agentSkills.size(), config.name());
        return config.withSkills(agentSkills);
    }

    private List<SkillDefinition> collectSkillsForAgent(String agentName,
                                                         List<SkillDefinition> globalSkills) {
        List<SkillDefinition> skills = new ArrayList<>();
        for (SkillDefinition skill : globalSkills) {
            if (isSkillApplicableToAgent(skill, agentName)) {
                skills.add(skill);
            }
        }
        return skills;
    }

    private boolean isSkillApplicableToAgent(SkillDefinition skill, String agentName) {
        String skillAgent = skill.metadata().get("agent");
        return skillAgent == null || skillAgent.equals(agentName);
    }

    private List<SkillDefinition> loadGlobalSkills() {
        Path skillsRoot = Path.of(skillsDirectory);
        if (!Files.isDirectory(skillsRoot)) {
            logger.debug("Global skills directory does not exist: {}", skillsRoot);
            return List.of();
        }

        List<Path> skillFiles = skillParser.discoverSkills(skillsRoot);
        if (skillFiles.isEmpty()) {
            return List.of();
        }

        List<SkillDefinition> skills = new ArrayList<>();
        for (Path skillFile : skillFiles) {
            try {
                SkillDefinition skill = skillParser.parse(skillFile);
                skills.add(skill);
                logger.info("Loaded global skill: {} from {}", skill.id(),
                    skillFile.getParent().getFileName());
            } catch (IOException | IllegalArgumentException | UncheckedIOException e) {
                logger.error("Failed to load skill from {}: {}", skillFile, e.getMessage(), e);
            }
        }

        logger.info("Loaded {} global skills from {}", skills.size(), skillsRoot);
        return List.copyOf(skills);
    }

    private boolean isAgentFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return filename.endsWith(".agent.md");
    }

    /// Lists all available agent names from all configured directories.
    public List<String> listAvailableAgents() throws IOException {
        Set<String> agentNames = new TreeSet<>();

        for (Path directory : agentDirectories) {
            if (!Files.exists(directory)) {
                continue;
            }

            List<Path> files = listAgentFiles(directory);
            for (Path file : files) {
                agentNames.add(extractAgentName(file));
            }
        }

        return List.copyOf(agentNames);
    }

    private List<Path> listAgentFiles(Path directory) throws IOException {
        return listAgentFiles(directory, null);
    }

    private List<Path> listAgentFiles(Path directory, Set<String> filter) throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths
                .filter(this::isAgentFile)
                .filter(path -> filter == null || filter.contains(extractAgentName(path)))
                .toList();
        }
    }

    private Optional<String> firstSuspiciousField(AgentConfig config) {
        if (containsSuspicious(config.systemPrompt())) {
            return Optional.of("role");
        }
        if (containsSuspicious(config.instruction())) {
            return Optional.of("instruction");
        }
        if (containsSuspicious(config.outputFormat())) {
            return Optional.of("output-format");
        }
        if (containsSuspicious(config.displayName())) {
            return Optional.of("display-name");
        }
        if (containsSuspicious(config.model())) {
            return Optional.of("model");
        }
        if (containsSuspicious(config.name())) {
            return Optional.of("name");
        }
        for (String focusArea : config.focusAreas()) {
            if (containsSuspicious(focusArea)) {
                return Optional.of("focus-areas");
            }
        }
        return Optional.empty();
    }

    private boolean containsSuspicious(String value) {
        return CustomInstructionSafetyValidator.containsSuspiciousPattern(value);
    }

    private String extractAgentName(Path file) {
        return AgentMarkdownParser.extractNameFromFilename(file.getFileName().toString());
    }

    List<Path> getAgentDirectories() {
        return agentDirectories;
    }
}
