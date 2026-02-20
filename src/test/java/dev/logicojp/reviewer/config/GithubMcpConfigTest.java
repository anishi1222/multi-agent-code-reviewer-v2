package dev.logicojp.reviewer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GithubMcpConfig")
class GithubMcpConfigTest {

    @Nested
    @DisplayName("コンストラクタ - デフォルト値")
    class DefaultValues {

        @Test
        @DisplayName("すべてnullの場合はデフォルト値が設定される")
        void allNullsUseDefaults() {
            var config = new GithubMcpConfig(null, null, null, null, null, null);

            assertThat(config.type()).isEqualTo("http");
            assertThat(config.url()).isEqualTo("https://api.githubcopilot.com/mcp/");
            assertThat(config.tools()).containsExactly("*");
            assertThat(config.headers()).isEmpty();
            assertThat(config.authHeaderName()).isEqualTo("Authorization");
            assertThat(config.authHeaderTemplate()).isEqualTo("Bearer {token}");
        }
    }

    @Nested
    @DisplayName("URL validation")
    class UrlValidation {

        @Test
        @DisplayName("http URL は拒否される")
        void rejectsNonHttpsUrl() {
            assertThatThrownBy(() -> new GithubMcpConfig(
                "http", "http://api.example.com/mcp/", List.of("*"),
                Map.of(), "Authorization", "Bearer {token}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must use HTTPS");
        }
    }

    @Nested
    @DisplayName("toMcpServer")
    class ToMcpServer {

        @Test
        @DisplayName("トークン付きでMCPサーバー設定を生成する")
        void generatesMcpServerWithToken() {
            var config = new GithubMcpConfig(
                "http", "https://api.example.com/mcp/",
                List.of("tool1"), Map.of(), "Authorization", "Bearer {token}");
            var server = config.toMcpServer("my-token");

            assertThat(server).containsEntry("type", "http");
            assertThat(server).containsEntry("url", "https://api.example.com/mcp/");
            assertThat(server).containsEntry("tools", List.of("tool1"));

            @SuppressWarnings("unchecked")
            var headers = (Map<String, String>) server.get("headers");
            assertThat(headers).containsEntry("Authorization", "Bearer my-token");
        }

        @Test
        @DisplayName("トークンがnullの場合はAuthorizationヘッダーを追加しない")
        void nullTokenSkipsAuthHeader() {
            var config = new GithubMcpConfig(null, null, null, null, null, null);
            var server = config.toMcpServer(null);

            @SuppressWarnings("unchecked")
            var headers = (Map<String, String>) server.get("headers");
            assertThat(headers).doesNotContainKey("Authorization");
        }

        @Test
        @DisplayName("空白トークンの場合はAuthorizationヘッダーを追加しない")
        void blankTokenSkipsAuthHeader() {
            var config = new GithubMcpConfig(null, null, null, null, null, null);
            var server = config.toMcpServer("  ");

            @SuppressWarnings("unchecked")
            var headers = (Map<String, String>) server.get("headers");
            assertThat(headers).doesNotContainKey("Authorization");
        }

        @Test
        @DisplayName("既存ヘッダーとAuthorizationヘッダーが結合される")
        void mergesExistingHeaders() {
            var config = new GithubMcpConfig(
                "http", "https://api.example.com/",
                List.of("*"), Map.of("X-Custom", "value"),
                "Authorization", "Bearer {token}");
            var server = config.toMcpServer("tok");

            @SuppressWarnings("unchecked")
            var headers = (Map<String, String>) server.get("headers");
            assertThat(headers).containsEntry("X-Custom", "value");
            assertThat(headers).containsEntry("Authorization", "Bearer tok");
        }

        @Test
        @DisplayName("MCPサーバーマップのtoStringでトークンをマスクする")
        void masksAuthorizationInToString() {
            var config = new GithubMcpConfig(
                "http", "https://api.example.com/",
                List.of("*"), Map.of(),
                "Authorization", "Bearer {token}");
            var server = config.toMcpServer("ghp_secret456");

            var str = server.toString();
            assertThat(str).contains("Bearer ***");
            assertThat(str).doesNotContain("ghp_secret456");
        }
    }

    @Nested
    @DisplayName("buildMcpServers")
    class BuildMcpServersTests {

        @Test
        @DisplayName("トークンと設定が揃っている場合はMCPサーバー設定を返す")
        void returnsMcpServersWhenInputsAreValid() {
            var config = new GithubMcpConfig(null, null, null, null, null, null);
            var servers = GithubMcpConfig.buildMcpServers("ghp_token", config);

            assertThat(servers).isPresent();
            assertThat(servers.orElseThrow()).containsKey("github");
        }

        @Test
        @DisplayName("トークンが空の場合はemptyを返す")
        void returnsEmptyWhenTokenEmpty() {
            assertThat(GithubMcpConfig.buildMcpServers("",
                new GithubMcpConfig(null, null, null, null, null, null))).isEmpty();
        }

        @Test
        @DisplayName("設定がnullの場合はemptyを返す")
        void returnsEmptyWhenConfigNull() {
            assertThat(GithubMcpConfig.buildMcpServers("ghp_token", null)).isEmpty();
        }
    }
}
