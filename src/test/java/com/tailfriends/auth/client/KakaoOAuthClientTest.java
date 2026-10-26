package com.tailfriends.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoOAuthClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final KakaoOAuthClient client = new KakaoOAuthClient(builder.build(), new OAuthProperties(
            new OAuthProperties.Registration("kakao-key", "kakao-secret"), null, null));

    @Test
    void 인가_코드로_토큰을_받고_사용자_정보를_가져온다() {
        server.expect(requestTo(KakaoOAuthClient.TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", "kakao-key",
                        "client_secret", "kakao-secret",
                        "redirect_uri", "https://app.example/callback",
                        "code", "auth-code")))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(KakaoOAuthClient.USER_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access"))
                .andRespond(withSuccess("""
                        { "id": 4215, "connected_at": "2026-10-20T00:00:00Z",
                          "kakao_account": { "email": "keeper@example.com", "profile": { "nickname": "망고집사" } } }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo user = client.fetchUser("auth-code", "https://app.example/callback");

        assertThat(user).isEqualTo(new OAuthUserInfo("4215", "망고집사", "keeper@example.com"));
        server.verify();
    }

    @Test
    void 닉네임과_이메일에_동의하지_않았으면_null로_돌려준다() {
        server.expect(requestTo(KakaoOAuthClient.TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(KakaoOAuthClient.USER_URL))
                .andRespond(withSuccess("{ \"id\": 4215 }", MediaType.APPLICATION_JSON));

        OAuthUserInfo user = client.fetchUser("auth-code", "https://app.example/callback");

        assertThat(user).isEqualTo(new OAuthUserInfo("4215", null, null));
    }

    @Test
    void 인가_코드가_틀리거나_만료되면_OAUTH_FAILED() {
        server.expect(requestTo(KakaoOAuthClient.TOKEN_URL))
                .andRespond(withBadRequest().body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchUser("expired", "https://app.example/callback"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_FAILED);
    }

    @Test
    void 앱_키가_없으면_소셜_서버를_호출하지_않고_실패한다() {
        KakaoOAuthClient unconfigured = new KakaoOAuthClient(builder.build(),
                new OAuthProperties(new OAuthProperties.Registration("", ""), null, null));

        assertThatThrownBy(() -> unconfigured.fetchUser("auth-code", "https://app.example/callback"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }
}
