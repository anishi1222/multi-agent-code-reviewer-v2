package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.target.ReviewTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewAgent")
class ReviewAgentTest {

    @Nested
    @DisplayName("resolveLocalSourceContentForPass")
    class ResolveLocalSourceContentForPass {

        @Test
        @DisplayName("ローカルターゲットの2パス目以降はnullを返す")
        void localTargetAfterSecondPassReturnsNull() {
            var target = ReviewTarget.local(Path.of("/tmp/repo"));

            String result = ReviewAgent.resolveLocalSourceContentForPass(target, "cached-content", 2);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ローカルターゲットの1パス目は元コンテンツを返す")
        void localTargetFirstPassReturnsOriginalContent() {
            var target = ReviewTarget.local(Path.of("/tmp/repo"));

            String result = ReviewAgent.resolveLocalSourceContentForPass(target, "cached-content", 1);

            assertThat(result).isEqualTo("cached-content");
        }

        @Test
        @DisplayName("GitHubターゲットは常に元コンテンツを返す")
        void githubTargetAlwaysReturnsOriginalContent() {
            var target = ReviewTarget.gitHub("owner/repo");

            String result = ReviewAgent.resolveLocalSourceContentForPass(target, "cached-content", 3);

            assertThat(result).isEqualTo("cached-content");
        }
    }
}
