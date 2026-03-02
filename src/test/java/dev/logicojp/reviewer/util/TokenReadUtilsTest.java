package dev.logicojp.reviewer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenReadUtils")
class TokenReadUtilsTest {

    @Test
    @DisplayName("パスワード入力経路ではtrimしてchar配列をクリアする")
    void trimsAndClearsPasswordBuffer() throws Exception {
        char[] password = "  secret-token  ".toCharArray();

        String token = TokenReadUtils.readTrimmedToken(
            () -> password,
            _ -> new byte[0],
            256
        );

        assertThat(token).isEqualTo("secret-token");
        assertThat(password).containsOnly('\0');
    }

    @Test
    @DisplayName("標準入力経路ではtrimしてbyte配列をクリアする")
    void trimsAndClearsStdinBuffer() throws Exception {
        byte[] input = "  stdin-token\n".getBytes(StandardCharsets.UTF_8);

        String token = TokenReadUtils.readTrimmedToken(
            () -> null,
            _ -> input,
            256
        );

        assertThat(token).isEqualTo("stdin-token");
        for (byte b : input) {
            assertThat(b).isZero();
        }
    }
}
