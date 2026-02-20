package dev.logicojp.reviewer.service;

import dev.logicojp.reviewer.agent.AgentConfig;
import dev.logicojp.reviewer.agent.AgentConfigLoader;
import dev.logicojp.reviewer.config.ReviewerConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Service for loading and managing agent configurations.
@Singleton
public class AgentService {

    private final ReviewerConfig reviewerConfig;
    private final TemplateService templateService;

    @Inject
    public AgentService(ReviewerConfig reviewerConfig, TemplateService templateService) {
        this.reviewerConfig = reviewerConfig;
        this.templateService = templateService;
    }

    /// Builds the list of agent directories to search.
    /// @param additionalDirs Additional directories specified via CLI
    /// @return List of directories to search for agents
    public List<Path> buildAgentDirectories(List<Path> additionalDirs) {
        List<Path> dirs = new ArrayList<>();

        for (String configuredDir : reviewerConfig.agents().directories()) {
            Path directory = Path.of(configuredDir);
            if (Files.exists(directory)) {
                dirs.add(directory);
            }
        }

        if (additionalDirs != null) {
            dirs.addAll(additionalDirs);
        }

        if (dirs.isEmpty()) {
            dirs.add(Path.of(reviewerConfig.agents().directories().getFirst()));
        }

        return List.copyOf(dirs);
    }

    /// Loads all agents from the specified directories.
    public Map<String, AgentConfig> loadAllAgents(List<Path> agentDirs) throws IOException {
        return createLoader(agentDirs).loadAllAgents();
    }

    /// Loads specific agents by name from the specified directories.
    public Map<String, AgentConfig> loadAgents(List<Path> agentDirs, List<String> agentNames) throws IOException {
        return createLoader(agentDirs).loadAgents(agentNames);
    }

    /// Lists all available agent names from the specified directories.
    public List<String> listAvailableAgents(List<Path> agentDirs) throws IOException {
        return createLoader(agentDirs).listAvailableAgents();
    }

    private AgentConfigLoader createLoader(List<Path> agentDirs) {
        String defaultOutputFormat = templateService.getDefaultOutputFormat();
        return AgentConfigLoader.builder(agentDirs)
            .skillConfig(reviewerConfig.skills())
            .defaultOutputFormat(defaultOutputFormat)
            .build();
    }
}
