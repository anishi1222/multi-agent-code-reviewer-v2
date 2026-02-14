package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.SkillConfig;
import dev.logicojp.reviewer.skill.SkillDefinition;
import dev.logicojp.reviewer.skill.SkillMarkdownParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
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
    
    /// Creates a loader with a single agents directory and default skill settings.
    public AgentConfigLoader(Path agentsDirectory) {
        this(List.of(agentsDirectory));
    }
    
    /// Creates a loader with multiple agent directories and default skill settings.
    /// Directories are searched in order; later directories override earlier ones.
    public AgentConfigLoader(List<Path> agentDirectories) {
        this(agentDirectories, new SkillConfig(null, null));
    }

    /// Creates a loader with multiple agent directories and skill configuration.
    public AgentConfigLoader(List<Path> agentDirectories, SkillConfig skillConfig) {
        this.agentDirectories = new ArrayList<>(agentDirectories);
        this.markdownParser = new AgentMarkdownParser();
        this.skillParser = new SkillMarkdownParser(skillConfig.filename());
        this.skillsDirectory = skillConfig.directory();
    }
    
    /// Loads all agent configurations from all configured directories.
    /// Skills are loaded from .github/skills/ (Agent Skills spec).
    /// @return Map of agent name to AgentConfig
    public Map<String, AgentConfig> loadAllAgents() throws IOException {
        return loadAgentsInternal(null);
    }
    
    /// Loads specific agents by name.
    /// Only parses agent files whose names match the requested list,
    /// avoiding unnecessary I/O and parsing of unneeded agent definitions.
    /// @param agentNames List of agent names to load
    /// @return Map of agent name to AgentConfig
    public Map<String, AgentConfig> loadAgents(List<String> agentNames) throws IOException {
        Map<String, AgentConfig> agents = loadAgentsInternal(new HashSet<>(agentNames));

        for (String name : agentNames) {
            if (!agents.containsKey(name)) {
                logger.warn("Agent not found: {}", name);
            }
        }

        return agents;
    }

    /// Internal implementation shared by loadAllAgents and loadAgents.
    /// @param filter If null, loads all agents; otherwise only agents with names in the set
    /// @return Map of agent name to AgentConfig
    private Map<String, AgentConfig> loadAgentsInternal(Set<String> filter) throws IOException {
        Map<String, AgentConfig> agents = new HashMap<>();
        List<SkillDefinition> globalSkills = loadGlobalSkills();

        for (Path directory : agentDirectories) {
            if (!Files.exists(directory)) {
                logger.debug("Agents directory does not exist: {}", directory);
                continue;
            }
            
            logger.info("Loading agents from: {}", directory);

            try (Stream<Path> paths = Files.list(directory)) {
                var files = paths
                    .filter(this::isAgentFile)
                    .filter(p -> filter == null || filter.contains(extractAgentName(p)))
                    .toList();

                for (Path file : files) {
                    try {
                        AgentConfig config = markdownParser.parse(file);
                        if (config != null) {
                            List<SkillDefinition> agentSkills = collectSkillsForAgent(
                                config.name(), globalSkills);
                            if (!agentSkills.isEmpty()) {
                                config = config.withSkills(agentSkills);
                                logger.info("Loaded {} skills for agent: {}", agentSkills.size(), config.name());
                            }
                            config.validateRequired();
                            agents.put(config.name(), config);
                            logger.info("Loaded agent: {} from {}", config.name(), file.getFileName());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to load agent from {}: {}", file, e.getMessage());
                    }
                }
            }
        }
        
        if (agents.isEmpty()) {
            logger.warn("No agents found in any configured directory");
        }
        
        return agents;
    }

    /// Collects skills for a specific agent from .github/skills/.
    ///
    /// Skills are matched to agents via the `metadata.agent` field.
    /// Skills without an agent metadata field are available to all agents.
    private List<SkillDefinition> collectSkillsForAgent(String agentName,
                                                         List<SkillDefinition> globalSkills) {
        List<SkillDefinition> skills = new ArrayList<>();

        for (SkillDefinition skill : globalSkills) {
            String skillAgent = skill.metadata().get("agent");
            if (skillAgent == null || skillAgent.equals(agentName)) {
                skills.add(skill);
            }
        }

        return skills;
    }

    /// Loads skills from the configured skills directory following the Agent Skills specification.
    /// Each skill is a directory containing a skill file.
    private List<SkillDefinition> loadGlobalSkills() {
        // Resolve skills directory relative to the working directory
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
            } catch (Exception e) {
                logger.error("Failed to load skill from {}: {}", skillFile, e.getMessage());
            }
        }

        logger.info("Loaded {} global skills from {}", skills.size(), skillsRoot);
        return List.copyOf(skills);
    }

    private boolean isAgentFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".agent.md");
    }
    
    /// Lists all available agent names from all configured directories.
    public List<String> listAvailableAgents() throws IOException {
        Set<String> agentNames = new TreeSet<>();
        
        for (Path directory : agentDirectories) {
            if (!Files.exists(directory)) {
                continue;
            }
            
            try (Stream<Path> paths = Files.list(directory)) {
                paths.filter(this::isAgentFile)
                    .map(this::extractAgentName)
                    .forEach(agentNames::add);
            }
        }
        
        return new ArrayList<>(agentNames);
    }
    
    private String extractAgentName(Path file) {
        String filename = file.getFileName().toString();
        
        if (filename.endsWith(".agent.md")) {
            return filename.substring(0, filename.length() - ".agent.md".length());
        }
        
        return filename;
    }
    
    /// Gets the list of configured agent directories.
    public List<Path> getAgentDirectories() {
        return Collections.unmodifiableList(agentDirectories);
    }
}
