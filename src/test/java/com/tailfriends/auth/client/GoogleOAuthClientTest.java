package com.tailfriends.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleOAuthClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GoogleOAuthClient client = new GoogleOAuthClient(builder.build(), new OAuthProperties(
            null, new OAuthProperties.Registration("google-id", "google-secret")));

    @Test
    void 인가_코드로_토큰을_받고_사용자_정보를_가져온다() {
        server.expect(requestTo(GoogleOAuthClient.TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", "google-id",
                        "client_secret", "google-secret",
                        "redirect_uri", "https://app.example/callback",
                        "code", "auth-code")))
                .andRespond(withSuccess("{\"access_token\":\"google-access\",\"id_token\":\"x.y.z\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(GoogleOAuthClient.USER_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer google-access"))
                .andRespond(withSuccess("""
                        { "sub": "1098765", "name": "김사육", "email": "keeper@gmail.com", "email_verified": true }
                        """, MediaType.APPLICATION_JSON));

        OAuthUserInfo user = client.fetchUser("auth-code", "https://app.example/callback");

        assertThat(user).isEqualTo(new OAuthUserInfo("1098765", "김사육", "keeper@gmail.com"));
        server.verify();
    }

    @Test
    void 사용자_정보_조회가_거부되면_OAUTH_FAILED() {
        server.expect(requestTo(GoogleOAuthClient.TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"google-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(GoogleOAuthClient.USER_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.fetchUser("auth-code", "https://app.example/callback"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_FAILED);
    }
}
