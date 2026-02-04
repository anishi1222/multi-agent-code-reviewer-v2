package dev.logicojp.reviewer.config;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the GitHub MCP server connection.
 */
@ConfigurationProperties("reviewer.mcp.github")
public class GithubMcpConfig {

    private String type = "http";
    private String url = "https://api.githubcopilot.com/mcp/";
    private List<String> tools = new ArrayList<>(List.of("*"));
    private Map<String, String> headers = new HashMap<>();
    private String authHeaderName = "Authorization";
    private String authHeaderTemplate = "Bearer ${token}";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = tools;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthHeaderTemplate() {
        return authHeaderTemplate;
    }

    public void setAuthHeaderTemplate(String authHeaderTemplate) {
        this.authHeaderTemplate = authHeaderTemplate;
    }

    public Map<String, Object> toMcpServer(String token) {
        Map<String, Object> server = new HashMap<>();
        server.put("type", type);
        server.put("url", url);
        server.put("tools", tools);

        Map<String, String> combinedHeaders = new HashMap<>(headers);
        if (authHeaderName != null && !authHeaderName.isBlank()
            && authHeaderTemplate != null && !authHeaderTemplate.isBlank()) {
            combinedHeaders.put(authHeaderName, authHeaderTemplate.replace("${token}", token));
        }
        server.put("headers", combinedHeaders);

        return server;
    }
}
