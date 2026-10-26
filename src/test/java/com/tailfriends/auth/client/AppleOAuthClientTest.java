package com.tailfriends.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AppleOAuthClientTest {

    private static final Instant NOW = Instant.parse("2026-10-26T03:00:00Z");
    private static final String BUNDLE_ID = "com.tailfriends.app";

    private final KeyPair keyPair = ecKeyPair();
    private final OAuthProperties.Apple apple = new OAuthProperties.Apple(BUNDLE_ID, "TEAM123456", "KEY1234567",
            Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AppleOAuthClient client = new AppleOAuthClient(builder.build(),
            new OAuthProperties(null, null, apple), Clock.fixed(NOW, ZoneOffset.UTC), new ObjectMapper());

    @Test
    void 인가_코드를_토큰으로_바꾸고_ID_토큰에서_사용자를_읽는다() {
        server.expect(requestTo(AppleOAuthClient.TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", BUNDLE_ID,
                        "code", "auth-code")))
                .andRespond(withSuccess(tokenResponse(idToken(AppleOAuthClient.ISSUER, BUNDLE_ID, NOW.plusSeconds(600))),
                        MediaType.APPLICATION_JSON));

        OAuthUserInfo user = client.fetchUser("auth-code", "https://app.example/callback");

        assertThat(user).isEqualTo(
                new OAuthUserInfo("001234.abcd.0123", null, "abc@privaterelay.appleid.com", "apple-refresh"));
        assertThat(user.toString()).doesNotContain("apple-refresh");
        server.verify();
    }

    @Test
    void client_secret은_팀_ID가_발급하고_번들_ID가_주체인_ES256_JWT다() {
        String secret = client.clientSecret(apple);

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .clock(() -> java.util.Date.from(NOW))
                .build()
                .parseSignedClaims(secret);
        assertThat(jws.getHeader().getKeyId()).isEqualTo("KEY1234567");
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(jws.getPayload().getIssuer()).isEqualTo("TEAM123456");
        assertThat(jws.getPayload().getSubject()).isEqualTo(BUNDLE_ID);
        assertThat(jws.getPayload().getAudience()).containsExactly(AppleOAuthClient.ISSUER);
        assertThat(jws.getPayload().getExpiration().toInstant()).isAfter(NOW);
    }

    @Test
    void 탈퇴할_때_refresh_token을_철회한다() {
        server.expect(requestTo(AppleOAuthClient.REVOKE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", BUNDLE_ID,
                        "token", "apple-refresh",
                        "token_type_hint", "refresh_token")))
                .andRespond(withSuccess());

        assertThat(client.revoke("apple-refresh")).isTrue();
        server.verify();
    }

    @Test
    void 애플이_철회를_거절하면_예외_없이_false() {
        server.expect(requestTo(AppleOAuthClient.REVOKE_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        assertThat(client.revoke("apple-refresh")).isFalse();
    }

    @Test
    void PEM_형식의_서명_키도_읽는다() {
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----";
        OAuthProperties.Apple pemApple = new OAuthProperties.Apple(BUNDLE_ID, "TEAM123456", "KEY1234567", pem);

        String secret = client.clientSecret(pemApple);

        assertThat(Jwts.parser().verifyWith(keyPair.getPublic()).clock(() -> java.util.Date.from(NOW)).build()
                .parseSignedClaims(secret).getPayload().getSubject()).isEqualTo(BUNDLE_ID);
    }

    @Test
    void 인가_코드가_만료되면_OAUTH_FAILED() {
        server.expect(requestTo(AppleOAuthClient.TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        assertOAuthFailed();
    }

    @Test
    void ID_토큰의_대상이_우리_앱이_아니면_OAUTH_FAILED() {
        respondWith(idToken(AppleOAuthClient.ISSUER, "com.other.app", NOW.plusSeconds(600)));

        assertOAuthFailed();
    }

    @Test
    void ID_토큰의_발급자가_애플이_아니면_OAUTH_FAILED() {
        respondWith(idToken("https://evil.example", BUNDLE_ID, NOW.plusSeconds(600)));

        assertOAuthFailed();
    }

    @Test
    void 만료된_ID_토큰이면_OAUTH_FAILED() {
        respondWith(idToken(AppleOAuthClient.ISSUER, BUNDLE_ID, NOW.minusSeconds(1)));

        assertOAuthFailed();
    }

    @Test
    void ID_토큰을_읽을_수_없으면_OAUTH_FAILED() {
        respondWith("not-a-jwt");

        assertOAuthFailed();
    }

    @Test
    void 앱_키가_없으면_애플_서버를_호출하지_않고_실패한다() {
        AppleOAuthClient unconfigured = new AppleOAuthClient(builder.build(),
                new OAuthProperties(null, null, new OAuthProperties.Apple(BUNDLE_ID, "", "", "")),
                Clock.fixed(NOW, ZoneOffset.UTC), new ObjectMapper());

        assertThatThrownBy(() -> unconfigured.fetchUser("auth-code", "https://app.example/callback"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    private void respondWith(String idToken) {
        server.expect(requestTo(AppleOAuthClient.TOKEN_URL))
                .andRespond(withSuccess(tokenResponse(idToken), MediaType.APPLICATION_JSON));
    }

    private void assertOAuthFailed() {
        assertThatThrownBy(() -> client.fetchUser("auth-code", "https://app.example/callback"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_FAILED);
    }

    private static String tokenResponse(String idToken) {
        return """
                { "access_token": "apple-access", "token_type": "Bearer", "expires_in": 3600,
                  "refresh_token": "apple-refresh", "id_token": "%s" }
                """.formatted(idToken);
    }

    /**
     * 클라이언트는 ID 토큰의 서명을 확인하지 않으므로 서명 부분은 아무 값이나 넣는다.
     */
    private static String idToken(String iss, String aud, Instant exp) {
        String header = base64Url("{\"alg\":\"RS256\",\"kid\":\"apple-key\"}");
        String payload = base64Url("""
                {"iss":"%s","aud":"%s","exp":%d,"iat":%d,"sub":"001234.abcd.0123",
                 "email":"abc@privaterelay.appleid.com","email_verified":true,"is_private_email":true}
                """.formatted(iss, aud, exp.getEpochSecond(), NOW.getEpochSecond()));
        return header + "." + payload + ".signature";
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static KeyPair ecKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
