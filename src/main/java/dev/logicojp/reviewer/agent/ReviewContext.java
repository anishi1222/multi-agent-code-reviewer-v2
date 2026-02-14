package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.GithubMcpConfig;
import dev.logicojp.reviewer.instruction.CustomInstruction;
import com.github.copilot.sdk.CopilotClient;

import java.util.List;
import java.util.Map;

/// Shared, immutable context for executing review agents.
///
/// Groups the common configuration that every {@link ReviewAgent} needs,
/// reducing the constructor parameter count from 10 to 2
/// ({@code AgentConfig} + {@code ReviewContext}).
///
/// @param client              The Copilot SDK client
/// @param githubToken         GitHub authentication token
/// @param githubMcpConfig     GitHub MCP server configuration
/// @param timeoutMinutes      Per-attempt timeout in minutes
/// @param idleTimeoutMinutes  Idle timeout in minutes (no-event threshold)
/// @param customInstructions  Custom instructions to inject into agent prompts
/// @param reasoningEffort     Reasoning effort level for reasoning models (nullable)
/// @param maxRetries          Maximum number of retries on failure
/// @param outputConstraints   Output constraints template content (nullable)
/// @param cachedMcpServers    Pre-built MCP server configuration map (nullable, cached for reuse)
public record ReviewContext(
    CopilotClient client,
    String githubToken,
    GithubMcpConfig githubMcpConfig,
    long timeoutMinutes,
    long idleTimeoutMinutes,
    List<CustomInstruction> customInstructions,
    String reasoningEffort,
    int maxRetries,
    String outputConstraints,
    Map<String, Object> cachedMcpServers
) {

    public ReviewContext(
        CopilotClient client,
        String githubToken,
        GithubMcpConfig githubMcpConfig,
        long timeoutMinutes,
        long idleTimeoutMinutes,
        List<CustomInstruction> customInstructions,
        String reasoningEffort,
        int maxRetries,
        String outputConstraints
    ) {
        this(client, githubToken, githubMcpConfig, timeoutMinutes, idleTimeoutMinutes,
            customInstructions, reasoningEffort, maxRetries, outputConstraints,
            (githubToken != null && !githubToken.isBlank() && githubMcpConfig != null)
                ? Map.of("github", githubMcpConfig.toMcpServer(githubToken))
                : null);
    }

    public ReviewContext {
        customInstructions = customInstructions != null ? List.copyOf(customInstructions) : List.of();
    }
}
