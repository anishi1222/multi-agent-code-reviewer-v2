package dev.logicojp.reviewer.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExitCodes")
class ExitCodesTest {

    @Test
    @DisplayName("OKは0である")
    void okIsZero() {
        assertThat(ExitCodes.OK).isZero();
    }

    @Test
    @DisplayName("SOFTWAREは1である")
    void softwareIsOne() {
        assertThat(ExitCodes.SOFTWARE).isEqualTo(1);
    }

    @Test
    @DisplayName("USAGEは2である")
    void usageIsTwo() {
        assertThat(ExitCodes.USAGE).isEqualTo(2);
    }

    @Test
    @DisplayName("全てのコードが異なる値である")
    void allCodesAreDistinct() {
        assertThat(ExitCodes.OK)
            .isNotEqualTo(ExitCodes.SOFTWARE)
            .isNotEqualTo(ExitCodes.USAGE);
        assertThat(ExitCodes.SOFTWARE).isNotEqualTo(ExitCodes.USAGE);
    }
}
