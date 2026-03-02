package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/// Configuration for the GitHub MCP server connection.
@ConfigurationProperties("reviewer.mcp.github")
public record GithubMcpConfig(
    String type,
    String url,
    List<String> tools,
    Map<String, String> headers,
    String authHeaderName,
    @Nullable String authHeaderTemplate
) {

    public GithubMcpConfig {
        type = ConfigDefaults.defaultIfBlank(type, "http");
        url = ConfigDefaults.defaultIfBlank(url, "https://api.githubcopilot.com/mcp/");
        validateUrl(url);
        tools = (tools == null || tools.isEmpty()) ? List.of("*") : List.copyOf(tools);
        headers = (headers == null) ? Map.of() : Map.copyOf(headers);
        authHeaderName = ConfigDefaults.defaultIfBlank(authHeaderName, "Authorization");
        authHeaderTemplate = ConfigDefaults.defaultIfBlank(authHeaderTemplate, "Bearer {token}");
    }

    private static void validateUrl(String url) {
        URI parsed = URI.create(url);
        String scheme = parsed.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("GitHub MCP URL must use HTTPS: " + url);
        }
        if (parsed.getHost() == null || parsed.getHost().isBlank()) {
            throw new IllegalArgumentException("GitHub MCP URL must include host: " + url);
        }
    }

    /// Type-safe intermediate representation of MCP server configuration.
    /// Provides compile-time safety within the application; converted to
    /// {@code Map<String, Object>} only at the SDK boundary.
    public record McpServerConfig(String type, String url, List<String> tools, Map<String, String> headers) {
        public McpServerConfig {
            tools = tools != null ? List.copyOf(tools) : List.of();
            headers = headers != null ? Map.copyOf(headers) : Map.of();
        }

        /// Converts to an immutable Map for SDK compatibility.
        public Map<String, Object> toMap() {
            return Map.of(
                "type", type,
                "url", url,
                "tools", tools,
                "headers", headers
            );
        }

        @Override
        public String toString() {
            Map<String, String> maskedHeaders = headers.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    entry -> SensitiveHeaderMasking.maskHeaderValue(entry.getKey(), entry.getValue())
                ));
            return "McpServerConfig{type='%s', url='%s', tools=%s, headers=%s}"
                .formatted(type, url, tools, maskedHeaders);
        }
    }

    /// Builds MCP server map from a token and config.
    /// Returns {@link Optional#empty()} when inputs are invalid.
    public static Optional<Map<String, Object>> buildMcpServers(String githubToken, GithubMcpConfig config) {
        if (canBuildMcpServers(githubToken, config)) {
            return Optional.of(Map.of("github", config.toMcpServer(githubToken)));
        }
        return Optional.empty();
    }

    private static boolean canBuildMcpServers(String githubToken, GithubMcpConfig config) {
        return githubToken != null && !githubToken.isBlank() && config != null;
    }

    public Map<String, Object> toMcpServer(String token) {
        Map<String, String> combinedHeaders = new HashMap<>(headers != null ? headers : Map.of());
        applyAuthHeader(token, combinedHeaders);
        Map<String, String> immutableHeaders = Map.copyOf(combinedHeaders);
        McpServerConfig config = new McpServerConfig(type, url, tools, immutableHeaders);
        Map<String, Object> rawMap = config.toMap();
        Map<String, Object> protectedMap = new HashMap<>(rawMap);
        protectedMap.put("headers", SensitiveHeaderMasking.wrapHeaders(immutableHeaders));
        return SensitiveHeaderMasking.wrapWithMaskedToString(protectedMap, config.toString());
    }

    private void applyAuthHeader(String token, Map<String, String> combinedHeaders) {
        if (token == null || token.isBlank()) {
            return;
        }
        String headerValue = authHeaderTemplate
            .replace("{token}", token);
        combinedHeaders.put(authHeaderName, headerValue);
    }
}
