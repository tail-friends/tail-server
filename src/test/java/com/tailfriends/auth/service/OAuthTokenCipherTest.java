package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class OAuthTokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());

    private final OAuthTokenCipher cipher = new OAuthTokenCipher(KEY);

    @Test
    void 암호화한_값을_같은_키로_풀면_원문이고_원문이_드러나지_않는다() {
        String encrypted = cipher.encrypt("apple-refresh-token");

        assertThat(encrypted).doesNotContain("apple-refresh-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("apple-refresh-token");
    }

    @Test
    void 같은_원문도_매번_다른_값으로_암호화한다() {
        assertThat(cipher.encrypt("apple-refresh-token")).isNotEqualTo(cipher.encrypt("apple-refresh-token"));
    }

    @Test
    void 다른_키나_바뀐_값은_풀지_못한다() {
        String encrypted = cipher.encrypt("apple-refresh-token");
        byte[] tampered = Base64.getDecoder().decode(encrypted);
        tampered[tampered.length - 1] ^= 1;

        assertThatThrownBy(() -> new OAuthTokenCipher(OTHER_KEY).decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(tampered)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 키가_없으면_서버는_뜨지만_암호화할_때_실패한다() {
        OAuthTokenCipher withoutKey = new OAuthTokenCipher("");

        assertThatThrownBy(() -> withoutKey.encrypt("apple-refresh-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAUTH_TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void 키가_32바이트가_아니거나_Base64가_아니면_만들_수_없다() {
        assertThatThrownBy(() -> new OAuthTokenCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new OAuthTokenCipher("not base64!"))
                .isInstanceOf(IllegalStateException.class);
    }
}
