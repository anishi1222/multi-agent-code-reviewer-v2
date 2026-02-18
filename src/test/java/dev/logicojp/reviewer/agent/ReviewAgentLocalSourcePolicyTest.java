package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.target.ReviewTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewAgent local source transfer policy")
class ReviewAgentLocalSourcePolicyTest {

    @Test
    @DisplayName("ローカルターゲットでは1パス目のみソースコンテンツを送信対象にする")
    void keepsLocalSourceOnlyForFirstPass() {
        var target = ReviewTarget.local(Path.of("/tmp/repo"));

        String firstPass = ReviewAgent.resolveLocalSourceContentForPass(target, "SOURCE", 1);
        String secondPass = ReviewAgent.resolveLocalSourceContentForPass(target, "SOURCE", 2);
        String thirdPass = ReviewAgent.resolveLocalSourceContentForPass(target, "SOURCE", 3);

        assertThat(firstPass).isEqualTo("SOURCE");
        assertThat(secondPass).isNull();
        assertThat(thirdPass).isNull();
    }

    @Test
    @DisplayName("リモートターゲットでは各パスでソースコンテンツ値を変更しない")
    void keepsSourceContentForRemoteTarget() {
        var target = ReviewTarget.gitHub("owner/repo");

        String firstPass = ReviewAgent.resolveLocalSourceContentForPass(target, "SOURCE", 1);
        String secondPass = ReviewAgent.resolveLocalSourceContentForPass(target, "SOURCE", 2);

        assertThat(firstPass).isEqualTo("SOURCE");
        assertThat(secondPass).isEqualTo("SOURCE");
    }
}
