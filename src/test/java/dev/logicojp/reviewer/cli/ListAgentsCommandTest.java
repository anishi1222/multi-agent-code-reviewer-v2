package dev.logicojp.reviewer.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListAgentsCommand")
class ListAgentsCommandTest {

    private static ListAgentsCommand createCommand(ByteArrayOutputStream out, ByteArrayOutputStream err) {
        var output = TestCliOutput.create(
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
        return new ListAgentsCommand(null, output);
    }

    @Test
    @DisplayName("--help は OK を返して usage を表示する")
    void helpReturnsOk() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var command = createCommand(out, err);

        int exitCode = command.execute(new String[]{"--help"});

        assertThat(exitCode).isEqualTo(ExitCodes.OK);
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("Usage: review list");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    @DisplayName("未知のオプションは USAGE を返す")
    void unknownOptionReturnsUsage() {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        var command = createCommand(out, err);

        int exitCode = command.execute(new String[]{"--unknown"});

        assertThat(exitCode).isEqualTo(ExitCodes.USAGE);
        assertThat(err.toString(StandardCharsets.UTF_8)).contains("Unknown option: --unknown");
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("Usage: review list");
    }
}
