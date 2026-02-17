package dev.logicojp.reviewer.instruction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InstructionSource")
class InstructionSourceTest {

    @Test
    @DisplayName("LOCAL_FILEが定義されている")
    void localFileExists() {
        assertThat(InstructionSource.LOCAL_FILE).isNotNull();
        assertThat(InstructionSource.LOCAL_FILE.name()).isEqualTo("LOCAL_FILE");
    }

    @Test
    @DisplayName("GITHUB_REPOSITORYが定義されている")
    void githubRepositoryExists() {
        assertThat(InstructionSource.GITHUB_REPOSITORY).isNotNull();
        assertThat(InstructionSource.GITHUB_REPOSITORY.name()).isEqualTo("GITHUB_REPOSITORY");
    }

    @Test
    @DisplayName("全Enum値の数が期待通りである")
    void expectedNumberOfValues() {
        assertThat(InstructionSource.values()).hasSize(2);
    }
}
