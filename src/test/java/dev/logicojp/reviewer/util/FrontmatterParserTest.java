package dev.logicojp.reviewer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FrontmatterParser")
class FrontmatterParserTest {

    @Nested
    @DisplayName("parse - 基本動作")
    class BasicParsing {

        @Test
        @DisplayName("標準的なフロントマターをパースする")
        void parsesStandardFrontmatter() {
            String content = """
                ---
                name: test
                description: "テスト説明"
                ---
                Body content here.""".stripIndent();

            var parsed = FrontmatterParser.parse(content);

            assertThat(parsed.hasFrontmatter()).isTrue();
            assertThat(parsed.metadata()).containsEntry("name", "test");
            assertThat(parsed.metadata()).containsEntry("description", "テスト説明");
            assertThat(parsed.body()).isEqualTo("Body content here.");
        }

        @Test
        @DisplayName("クォートなしの値をそのまま保持する")
        void preservesUnquotedValues() {
            String content = """
                ---
                model: claude-sonnet-4
                ---
                Content.""".stripIndent();

            var parsed = FrontmatterParser.parse(content);

            assertThat(parsed.get("model")).isEqualTo("claude-sonnet-4");
        }
    }

    @Nested
    @DisplayName("parse - フロントマターなし")
    class NoFrontmatter {

        @Test
        @DisplayName("---で始まらないコンテンツはフロントマターなしとして扱う")
        void noFrontmatterWhenNoDashes() {
            String content = "# Title\n\nBody content.";

            var parsed = FrontmatterParser.parse(content);

            assertThat(parsed.hasFrontmatter()).isFalse();
            assertThat(parsed.metadata()).isEmpty();
            assertThat(parsed.body()).isEqualTo(content);
        }

        @Test
        @DisplayName("閉じ---がない場合はフロントマターなしとして扱う")
        void noFrontmatterWhenNoClosingDelimiter() {
            var parsed = FrontmatterParser.parse("---\nname: test\nNo closing delimiter.");
            assertThat(parsed.hasFrontmatter()).isFalse();
        }

        @Test
        @DisplayName("null入力で空の結果を返す")
        void handlesNullInput() {
            var parsed = FrontmatterParser.parse(null);

            assertThat(parsed.hasFrontmatter()).isFalse();
            assertThat(parsed.body()).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseNestedBlock")
    class NestedBlockParsing {

        @Test
        @DisplayName("metadataブロックを正しくパースする")
        void parsesMetadataBlock() {
            String frontmatter = """
                name: skill
                metadata:
                  agent: security
                  version: "1.0"
                description: test""".stripIndent();

            Map<String, String> metadata = FrontmatterParser.parseNestedBlock(frontmatter, "metadata");

            assertThat(metadata).containsEntry("agent", "security");
            assertThat(metadata).containsEntry("version", "1.0");
        }

        @Test
        @DisplayName("ネストブロックが存在しない場合は空マップを返す")
        void returnsEmptyWhenBlockMissing() {
            Map<String, String> result = FrontmatterParser.parseNestedBlock(
                "name: test\ndescription: desc", "metadata");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractRawFrontmatter")
    class ExtractRawFrontmatter {

        @Test
        @DisplayName("生のフロントマターテキストを抽出する")
        void extractsRawText() {
            String content = """
                ---
                name: test
                model: gpt-4
                ---
                Body.""".stripIndent();

            String raw = FrontmatterParser.extractRawFrontmatter(content);

            assertThat(raw).contains("name: test");
            assertThat(raw).contains("model: gpt-4");
            assertThat(raw).doesNotContain("Body.");
        }

        @Test
        @DisplayName("フロントマターがない場合はnullを返す")
        void returnsNullWhenMissing() {
            assertThat(FrontmatterParser.extractRawFrontmatter("No frontmatter")).isNull();
        }
    }

    @Nested
    @DisplayName("getOrDefault")
    class GetOrDefault {

        @Test
        @DisplayName("存在するキーの値を返す")
        void returnsExistingValue() {
            String content = """
                ---
                name: test
                ---
                Body.""".stripIndent();

            var parsed = FrontmatterParser.parse(content);

            assertThat(parsed.getOrDefault("name", "default")).isEqualTo("test");
        }

        @Test
        @DisplayName("存在しないキーにはデフォルト値を返す")
        void returnsDefaultForMissing() {
            String content = """
                ---
                name: test
                ---
                Body.""".stripIndent();

            var parsed = FrontmatterParser.parse(content);

            assertThat(parsed.getOrDefault("missing", "fallback")).isEqualTo("fallback");
        }
    }

    @Nested
    @DisplayName("YAMLセキュリティ制限")
    class YamlSecurityLimits {

        @Test
        @DisplayName("LoaderOptions制限値が明示設定されている")
        void hasExplicitLoaderLimits() {
            assertThat(FrontmatterParser.MAX_ALIASES_FOR_COLLECTIONS).isEqualTo(10);
            assertThat(FrontmatterParser.FRONTMATTER_CODEPOINT_LIMIT).isEqualTo(64 * 1024);
        }

        @Test
        @DisplayName("エイリアス過多のYAMLでも例外を外部へ伝播しない")
        void handlesTooManyAliasesSafely() {
            String content = """
                ---
                name: test
                a: &a ["x"]
                b: [*a,*a,*a,*a,*a,*a,*a,*a,*a,*a,*a,*a]
                ---
                Body content here.""".stripIndent();

            var parsed = FrontmatterParser.parse(content);

            assertThat(parsed.hasFrontmatter()).isTrue();
            assertThat(parsed.get("name")).isEqualTo("test");
            assertThat(parsed.body()).isEqualTo("Body content here.");
        }
    }
}
