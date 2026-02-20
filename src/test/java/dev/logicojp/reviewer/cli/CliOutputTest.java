package dev.logicojp.reviewer.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CliOutput")
class CliOutputTest {

    private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    private final CliOutput output = new CliOutput(
            new PrintStream(outStream, true, StandardCharsets.UTF_8),
            new PrintStream(errStream, true, StandardCharsets.UTF_8));

    @Nested
    @DisplayName("println")
    class PrintlnTest {

        @Test
        @DisplayName("標準出力に書き込まれる")
        void writesToStdout() {
            output.println("hello");
            assertThat(outStream.toString(StandardCharsets.UTF_8)).contains("hello");
        }
    }

    @Nested
    @DisplayName("errorln")
    class ErrorlnTest {

        @Test
        @DisplayName("標準エラーに書き込まれる")
        void writesToStderr() {
            output.errorln("error message");
            assertThat(errStream.toString(StandardCharsets.UTF_8)).contains("error message");
        }
    }

    @Nested
    @DisplayName("out/err アクセサ")
    class AccessorTest {

        @Test
        @DisplayName("outでPrintStreamが取得できる")
        void outReturnsStream() {
            assertThat(output.out()).isNotNull();
            output.out().print("direct");
            assertThat(outStream.toString(StandardCharsets.UTF_8)).contains("direct");
        }

        @Test
        @DisplayName("errでPrintStreamが取得できる")
        void errReturnsStream() {
            assertThat(output.err()).isNotNull();
            output.err().print("direct-err");
            assertThat(errStream.toString(StandardCharsets.UTF_8)).contains("direct-err");
        }
    }
}
