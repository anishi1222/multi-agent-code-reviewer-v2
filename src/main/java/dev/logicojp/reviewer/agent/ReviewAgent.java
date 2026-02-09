package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.GithubMcpConfig;
import dev.logicojp.reviewer.instruction.CustomInstruction;
import dev.logicojp.reviewer.report.ReviewResult;
import dev.logicojp.reviewer.target.LocalFileProvider;
import dev.logicojp.reviewer.target.ReviewTarget;
import com.github.copilot.sdk.*;
import com.github.copilot.sdk.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static dev.logicojp.reviewer.report.SummaryGenerator.resolveReasoningEffort;

/**
 * Executes a code review using the Copilot SDK with a specific agent configuration.
 */
public class ReviewAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewAgent.class);
    
    private final AgentConfig config;
    private final CopilotClient client;
    private final String githubToken;
    private final GithubMcpConfig githubMcpConfig;
    private final long timeoutMinutes;
    private final List<CustomInstruction> customInstructions;
    private final String reasoningEffort;

    public ReviewAgent(AgentConfig config,
                       CopilotClient client,
                       String githubToken,
                       GithubMcpConfig githubMcpConfig,
                       long timeoutMinutes,
                       List<CustomInstruction> customInstructions,
                       String reasoningEffort) {
        this.config = config;
        this.client = client;
        this.githubToken = githubToken;
        this.githubMcpConfig = githubMcpConfig;
        this.timeoutMinutes = timeoutMinutes;
        this.customInstructions = customInstructions != null ? List.copyOf(customInstructions) : List.of();
        this.reasoningEffort = reasoningEffort;
    }
    
    /**
     * Executes the review for the given target.
     * @param target The target to review (GitHub repository or local directory)
     * @return ReviewResult containing the review content
     */
    public CompletableFuture<ReviewResult> review(ReviewTarget target) {
        return review(target, null);
    }

    public CompletableFuture<ReviewResult> review(ReviewTarget target, Executor executor) {
        if (executor == null) {
            return CompletableFuture.supplyAsync(() -> runReview(target));
        }
        return CompletableFuture.supplyAsync(() -> runReview(target), executor);
    }

    private ReviewResult runReview(ReviewTarget target) {
        try {
            return executeReview(target);
        } catch (Exception e) {
            logger.error("Review failed for agent {}: {}", config.getName(), e.getMessage(), e);
            return ReviewResult.builder()
                .agentConfig(config)
                .repository(target.getDisplayName())
                .success(false)
                .errorMessage(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
    
    private ReviewResult executeReview(ReviewTarget target) throws Exception {
        logger.info("Starting review with agent: {} for target: {}", 
            config.getName(), target.getDisplayName());
        
        // Java 21+: Pattern matching for switch with record patterns
        return switch (target) {
            case ReviewTarget.LocalTarget(Path directory) -> executeLocalReview(directory, target);
            case ReviewTarget.GitHubTarget(String repository) -> executeGitHubReview(repository, target);
        };
    }
    
    private ReviewResult executeGitHubReview(String repository, ReviewTarget target) throws Exception {
        
        // Configure GitHub MCP server for repository access
        Map<String, Object> githubMcp = githubMcpConfig.toMcpServer(githubToken);
        
        // Create session with agent configuration
        String systemPrompt = buildSystemPromptWithCustomInstruction();
        var sessionConfig = new SessionConfig()
            .setModel(config.getModel())
            .setSystemMessage(new SystemMessageConfig()
                .setMode(SystemMessageMode.APPEND)
                .setContent(systemPrompt))
            .setMcpServers(Map.of("github", githubMcp));

        // Explicitly set reasoning effort for reasoning models (e.g. Claude Opus)
        String effort = resolveReasoningEffort(config.getModel(), reasoningEffort);
        if (effort != null) {
            logger.info("Setting reasoning effort '{}' for model: {}", effort, config.getModel());
            sessionConfig.setReasoningEffort(effort);
        }

        var session = client.createSession(sessionConfig).get(timeoutMinutes, TimeUnit.MINUTES);
        long timeoutMs = TimeUnit.MINUTES.toMillis(timeoutMinutes);
        
        try {
            // Build the instruction
            String instruction = config.buildInstruction(repository);
            
            // Execute the review
            logger.debug("Sending instruction to agent: {}", config.getName());
            var response = session
                .sendAndWait(new MessageOptions().setPrompt(instruction), timeoutMs)
                .get(timeoutMinutes, TimeUnit.MINUTES);
            
            String content = response.getData().getContent();
            logger.info("Review completed for agent: {}", config.getName());
            
            return ReviewResult.builder()
                .agentConfig(config)
                .repository(repository)
                .content(content)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
                
        } finally {
            session.close();
        }
    }
    
    private ReviewResult executeLocalReview(Path localPath, ReviewTarget target) throws Exception {
        
        // Collect files from local directory
        LocalFileProvider fileProvider = new LocalFileProvider(localPath);
        var localFiles = fileProvider.collectFiles();
        String sourceContent = fileProvider.generateReviewContent(localFiles);
        String directorySummary = fileProvider.generateDirectorySummary(localFiles);
        
        logger.info("Collected source files from local directory: {}", localPath);
        logger.debug("Directory summary:\n{}", directorySummary);
        
        // Create session without MCP servers (no external tools needed for local review)
        String systemPrompt = buildSystemPromptWithCustomInstruction();
        var sessionConfig = new SessionConfig()
            .setModel(config.getModel())
            .setSystemMessage(new SystemMessageConfig()
                .setMode(SystemMessageMode.APPEND)
                .setContent(systemPrompt));

        // Explicitly set reasoning effort for reasoning models (e.g. Claude Opus)
        String effort = resolveReasoningEffort(config.getModel(), reasoningEffort);
        if (effort != null) {
            logger.info("Setting reasoning effort '{}' for model: {}", effort, config.getModel());
            sessionConfig.setReasoningEffort(effort);
        }

        var session = client.createSession(sessionConfig).get(timeoutMinutes, TimeUnit.MINUTES);
        long timeoutMs = TimeUnit.MINUTES.toMillis(timeoutMinutes);
        
        try {
            // Build the instruction with embedded source code
            String instruction = config.buildLocalInstruction(target.getDisplayName(), sourceContent);
            
            // Execute the review
            logger.debug("Sending local instruction to agent: {}", config.getName());
            var response = session
                .sendAndWait(new MessageOptions().setPrompt(instruction), timeoutMs)
                .get(timeoutMinutes, TimeUnit.MINUTES);
            
            String content = response.getData().getContent();
            logger.info("Local review completed for agent: {}", config.getName());
            
            return ReviewResult.builder()
                .agentConfig(config)
                .repository(target.getDisplayName())
                .content(content)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
                
        } finally {
            session.close();
        }
    }
    
    /**
     * Builds the system prompt including custom instructions if available.
     * Each custom instruction is rendered with its scope metadata (applyTo, description)
     * from GitHub Copilot per-scope instruction files (.github/instructions/*.instructions.md).
     */
    private String buildSystemPromptWithCustomInstruction() {
        StringBuilder sb = new StringBuilder();
        sb.append(config.buildFullSystemPrompt());
        
        if (!customInstructions.isEmpty()) {
            for (CustomInstruction instruction : customInstructions) {
                if (!instruction.isEmpty()) {
                    sb.append("\n\n");
                    sb.append(instruction.toPromptSection());
                    logger.debug("Applied custom instruction from {} to agent: {}", 
                        instruction.sourcePath(), config.getName());
                }
            }
        }
        
        return sb.toString();
    }
}
