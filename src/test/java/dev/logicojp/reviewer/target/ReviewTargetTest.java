package dev.logicojp.reviewer.target;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReviewTarget")
class ReviewTargetTest {

    @Nested
    @DisplayName("gitHub - ファクトリメソッド")
    class GitHubFactory {

        @Test
        @DisplayName("有効なowner/repo形式でGitHubTargetを生成する")
        void createsGitHubTarget() {
            ReviewTarget target = ReviewTarget.gitHub("owner/repo");

            assertThat(target).isInstanceOf(ReviewTarget.GitHubTarget.class);
            assertThat(target.displayName()).isEqualTo("owner/repo");
            assertThat(target.isLocal()).isFalse();
        }

        @Test
        @DisplayName("nullの場合は例外をスローする")
        void throwsForNull() {
            assertThatThrownBy(() -> ReviewTarget.gitHub(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空文字列の場合は例外をスローする")
        void throwsForBlank() {
            assertThatThrownBy(() -> ReviewTarget.gitHub("  "))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("不正な形式の場合は例外をスローする")
        void throwsForInvalidFormat() {
            assertThatThrownBy(() -> ReviewTarget.gitHub("no-slash"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("パストラバーサルを含む場合は例外をスローする")
        void throwsForTraversal() {
            assertThatThrownBy(() -> ReviewTarget.gitHub("../attack"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ドットセグメントは拒否される")
        void rejectsDotSegments() {
            assertThatThrownBy(() -> ReviewTarget.gitHub("./repo"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("local - ファクトリメソッド")
    class LocalFactory {

        @Test
        @DisplayName("有効なPathでLocalTargetを生成する")
        void createsLocalTarget(@TempDir Path tempDir) {
            ReviewTarget target = ReviewTarget.local(tempDir);

            assertThat(target).isInstanceOf(ReviewTarget.LocalTarget.class);
            assertThat(target.isLocal()).isTrue();
            assertThat(target.localPath()).isPresent();
        }

        @Test
        @DisplayName("nullの場合は例外をスローする")
        void throwsForNull() {
            assertThatThrownBy(() -> ReviewTarget.local(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("displayName")
    class DisplayNameTests {

        @Test
        @DisplayName("GitHubTargetはリポジトリ名を返す")
        void gitHubReturnsRepository() {
            assertThat(ReviewTarget.gitHub("owner/repo").displayName())
                .isEqualTo("owner/repo");
        }

        @Test
        @DisplayName("LocalTargetはディレクトリ名を返す")
        void localReturnsDirectoryName(@TempDir Path tempDir) {
            assertThat(ReviewTarget.local(tempDir).displayName())
                .isEqualTo(tempDir.getFileName().toString());
        }
    }

    @Nested
    @DisplayName("localPath")
    class LocalPathTests {

        @Test
        @DisplayName("GitHubTargetはemptyを返す")
        void gitHubReturnsEmpty() {
            assertThat(ReviewTarget.gitHub("owner/repo").localPath()).isEmpty();
        }

        @Test
        @DisplayName("LocalTargetはPathを返す")
        void localReturnsPath(@TempDir Path tempDir) {
            assertThat(ReviewTarget.local(tempDir).localPath())
                .isPresent()
                .hasValue(tempDir);
        }
    }

    @Nested
    @DisplayName("repositorySubPath")
    class RepositorySubPath {

        @Test
        @DisplayName("GitHubTargetはリポジトリのサブパスを返す")
        void gitHubReturnsSubPath() {
            Path subPath = ReviewTarget.gitHub("owner/repo").repositorySubPath();
            assertThat(subPath).isEqualTo(Path.of("owner/repo"));
        }

        @Test
        @DisplayName("LocalTargetはディレクトリ名のPathを返す")
        void localReturnsDirectoryNamePath(@TempDir Path tempDir) {
            Path subPath = ReviewTarget.local(tempDir).repositorySubPath();
            assertThat(subPath.toString()).isEqualTo(tempDir.getFileName().toString());
        }
    }

    @Nested
    @DisplayName("パターンマッチング")
    class PatternMatching {

        @Test
        @DisplayName("switch式でGitHubTargetをマッチングできる")
        void matchesGitHubTarget() {
            ReviewTarget target = ReviewTarget.gitHub("owner/repo");
            String result = switch (target) {
                case ReviewTarget.GitHubTarget(String repository) -> "GitHub: " + repository;
                case ReviewTarget.LocalTarget(_) -> "Local";
            };
            assertThat(result).isEqualTo("GitHub: owner/repo");
        }

        @Test
        @DisplayName("switch式でLocalTargetをマッチングできる")
        void matchesLocalTarget(@TempDir Path tempDir) {
            ReviewTarget target = ReviewTarget.local(tempDir);
            String result = switch (target) {
                case ReviewTarget.GitHubTarget(_) -> "GitHub";
                case ReviewTarget.LocalTarget(Path directory) -> "Local: " + directory;
            };
            assertThat(result).startsWith("Local: ");
        }
    }
}
