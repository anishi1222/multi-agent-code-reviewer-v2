package dev.logicojp.reviewer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GithubMcpConfig")
class GithubMcpConfigTest {

    @Nested
    @DisplayName("コンストラクタ - デフォルト値")
    class DefaultValues {

        @Test
        @DisplayName("すべてnullの場合はデフォルト値が設定される")
        void allNullsUseDefaults() {
            GithubMcpConfig config = new GithubMcpConfig(null, null, null, null, null, null);
            assertThat(config.type()).isEqualTo("http");
            assertThat(config.url()).isEqualTo("https://api.githubcopilot.com/mcp/");
            assertThat(config.tools()).containsExactly("*");
            assertThat(config.headers()).isEmpty();
            assertThat(config.authHeaderName()).isEqualTo("Authorization");
            assertThat(config.authHeaderTemplate()).isEqualTo("Bearer {token}");
        }
    }

    @Nested
    @DisplayName("toMcpServer")
    class ToMcpServer {

        @Test
        @DisplayName("トークン付きでMCPサーバー設定を生成する")
        void generatesMcpServerWithToken() {
            GithubMcpConfig config = new GithubMcpConfig(
                "http", "https://api.example.com/mcp/",
                List.of("tool1"), Map.of(), "Authorization", "Bearer {token}");
            Map<String, Object> server = config.toMcpServer("my-token");

            assertThat(server).containsEntry("type", "http");
            assertThat(server).containsEntry("url", "https://api.example.com/mcp/");
            assertThat(server).containsEntry("tools", List.of("tool1"));

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) server.get("headers");
            assertThat(headers).containsEntry("Authorization", "Bearer my-token");
        }

        @Test
        @DisplayName("トークンがnullの場合はAuthorizationヘッダーを追加しない")
        void nullTokenSkipsAuthHeader() {
            GithubMcpConfig config = new GithubMcpConfig(null, null, null, null, null, null);
            Map<String, Object> server = config.toMcpServer(null);

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) server.get("headers");
            assertThat(headers).doesNotContainKey("Authorization");
        }

        @Test
        @DisplayName("空白トークンの場合はAuthorizationヘッダーを追加しない")
        void blankTokenSkipsAuthHeader() {
            GithubMcpConfig config = new GithubMcpConfig(null, null, null, null, null, null);
            Map<String, Object> server = config.toMcpServer("  ");

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) server.get("headers");
            assertThat(headers).doesNotContainKey("Authorization");
        }

        @Test
        @DisplayName("既存ヘッダーとAuthorizationヘッダーが結合される")
        void mergesExistingHeaders() {
            GithubMcpConfig config = new GithubMcpConfig(
                "http", "https://api.example.com/",
                List.of("*"), Map.of("X-Custom", "value"),
                "Authorization", "Bearer {token}");
            Map<String, Object> server = config.toMcpServer("tok");

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) server.get("headers");
            assertThat(headers).containsEntry("X-Custom", "value");
            assertThat(headers).containsEntry("Authorization", "Bearer tok");
        }

        @Test
        @DisplayName("${token}プレースホルダーも置換される")
        void replaceDollarBraceTokenPlaceholder() {
            GithubMcpConfig config = new GithubMcpConfig(
                "http", "https://api.example.com/",
                List.of("*"), Map.of(),
                "Authorization", "token ${token}");
            Map<String, Object> server = config.toMcpServer("abc123");

            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) server.get("headers");
            assertThat(headers).containsEntry("Authorization", "token abc123");
        }
    }

    @Nested
    @DisplayName("McpServerConfig")
    class McpServerConfigTests {

        @Test
        @DisplayName("toMapは型安全な表現からMapに変換する")
        void toMapConvertsCorrectly() {
            var mcpConfig = new GithubMcpConfig.McpServerConfig(
                "http", "https://api.example.com/",
                List.of("tool1", "tool2"),
                Map.of("X-Header", "value"));
            Map<String, Object> map = mcpConfig.toMap();

            assertThat(map).containsEntry("type", "http");
            assertThat(map).containsEntry("url", "https://api.example.com/");
            assertThat(map).containsEntry("tools", List.of("tool1", "tool2"));
            assertThat(map).containsEntry("headers", Map.of("X-Header", "value"));
        }
    }
}
