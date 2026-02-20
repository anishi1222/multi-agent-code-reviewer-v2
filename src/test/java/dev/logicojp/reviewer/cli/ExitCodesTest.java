package dev.logicojp.reviewer.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExitCodes")
class ExitCodesTest {

    @Test
    @DisplayName("OKは0")
    void okIsZero() {
        assertThat(ExitCodes.OK).isZero();
    }

    @Test
    @DisplayName("USAGEは2")
    void usageIsTwo() {
        assertThat(ExitCodes.USAGE).isEqualTo(2);
    }

    @Test
    @DisplayName("SOFTWAREは1")
    void softwareIsOne() {
        assertThat(ExitCodes.SOFTWARE).isEqualTo(1);
    }
}
