package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;

import java.net.URI;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /// Builds MCP server map from a token and config.
    /// Returns empty when inputs are invalid.
    public static Optional<Map<String, Object>> buildMcpServers(String githubToken, GithubMcpConfig config) {
        if (githubToken != null && !githubToken.isBlank() && config != null) {
            return Optional.of(Map.of("github", config.toMcpServer(githubToken)));
        }
        return Optional.empty();
    }

    /// Builds a type-safe MCP server configuration map for SDK consumption.
    /// The returned map masks sensitive header values in toString() to prevent
    /// token leakage via SDK/framework debug logging.
    public Map<String, Object> toMcpServer(String token) {
        var combinedHeaders = new HashMap<>(headers != null ? headers : Map.of());
        applyAuthHeader(token, combinedHeaders);
        Map<String, String> immutableHeaders = Map.copyOf(combinedHeaders);

        String maskedHeadersStr = buildMaskedHeadersString(immutableHeaders);
        Map<String, String> wrappedHeaders = maskedToStringMap(immutableHeaders, maskedHeadersStr);

        Map<String, Object> raw = Map.of(
            "type", type,
            "url", url,
            "tools", tools,
            "headers", wrappedHeaders
        );
        String maskedConfigStr = "McpServerConfig{type='%s', url='%s', tools=%s, headers=%s}"
            .formatted(type, url, tools, maskedHeadersStr);
        return maskedToStringMap(raw, maskedConfigStr);
    }

    private void applyAuthHeader(String token, Map<String, String> combinedHeaders) {
        if (token == null || token.isBlank()) {
            return;
        }
        String headerValue = authHeaderTemplate.replace("{token}", token);
        combinedHeaders.put(authHeaderName, headerValue);
    }

    // --- Token masking utilities ---

    /// Creates an immutable map wrapper that delegates all operations to the
    /// source map but returns a masked string from toString().
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> maskedToStringMap(Map<K, V> source, String maskedString) {
        Map<K, V> immutable = (Map<K, V>) Map.copyOf((Map<?, ?>) source);
        return new AbstractMap<>() {
            @Override
            public Set<Entry<K, V>> entrySet() {
                return immutable.entrySet();
            }

            @Override
            public V get(Object key) {
                return immutable.get(key);
            }

            @Override
            public int size() {
                return immutable.size();
            }

            @Override
            public boolean isEmpty() {
                return immutable.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return immutable.containsKey(key);
            }

            @Override
            public String toString() {
                return maskedString;
            }
        };
    }

    private static String buildMaskedHeadersString(Map<String, String> headers) {
        return headers.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> maskHeaderValue(entry.getKey(), entry.getValue())
            ))
            .toString();
    }

    private static String maskHeaderValue(String headerName, String value) {
        if (!isSensitiveHeaderName(headerName)) {
            return value;
        }
        if (value == null || value.isBlank()) {
            return "***";
        }
        int spaceIndex = value.indexOf(' ');
        return spaceIndex > 0 ? value.substring(0, spaceIndex) + " ***" : "***";
    }

    private static boolean isSensitiveHeaderName(String headerName) {
        String normalized = headerName == null ? "" : headerName.toLowerCase(Locale.ROOT);
        return normalized.contains("authorization") || normalized.contains("token");
    }
}
