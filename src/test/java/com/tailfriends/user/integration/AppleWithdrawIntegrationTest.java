package com.tailfriends.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.auth.client.AppleOAuthClient;
import com.tailfriends.auth.client.KakaoOAuthClient;
import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.service.OAuthTokenCipher;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 애플·카카오 서버 호출만 가짜로 바꾸고, 토큰 저장(암호화)과 탈퇴 후 철회를 실제 DB로 확인한다.
 */
class AppleWithdrawIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OAuthTokenCipher oauthTokenCipher;

    @MockitoBean
    private AppleOAuthClient appleOAuthClient;

    @MockitoBean
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        given(appleOAuthClient.provider()).willReturn(Provider.APPLE);
        given(kakaoOAuthClient.provider()).willReturn(Provider.KAKAO);
    }

    @Test
    void 애플_로그인하면_refresh_token을_암호화해_두고_탈퇴하면_계정을_지운_뒤_철회한다() throws Exception {
        String refreshToken = "apple-refresh-" + UUID.randomUUID();
        String login = login("APPLE", new OAuthUserInfo(UUID.randomUUID().toString(), null, null, refreshToken));
        long userId = ((Number) JsonPath.read(login, "$.data.user.id")).longValue();

        String stored = storedToken(userId);
        assertThat(stored).doesNotContain(refreshToken);
        assertThat(oauthTokenCipher.decrypt(stored)).isEqualTo(refreshToken);
        given(appleOAuthClient.revoke(refreshToken)).willReturn(true);

        withdraw(login);

        verify(appleOAuthClient).revoke(refreshToken);
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(countTokens(userId)).isZero();
    }

    @Test
    void 다시_로그인하면_새_토큰으로_바꾸고_탈퇴할_때_새_토큰을_철회한다() throws Exception {
        String appleId = UUID.randomUUID().toString();
        login("APPLE", new OAuthUserInfo(appleId, null, null, "old-refresh"));
        String second = login("APPLE", new OAuthUserInfo(appleId, null, null, "new-refresh"));
        long userId = ((Number) JsonPath.read(second, "$.data.user.id")).longValue();
        assertThat(countTokens(userId)).isEqualTo(1);

        withdraw(second);

        verify(appleOAuthClient).revoke("new-refresh");
        verify(appleOAuthClient, never()).revoke("old-refresh");
    }

    @Test
    void 철회에_실패해도_탈퇴는_완료된다() throws Exception {
        String login = login("APPLE", new OAuthUserInfo(UUID.randomUUID().toString(), null, null, "apple-refresh"));
        long userId = ((Number) JsonPath.read(login, "$.data.user.id")).longValue();
        given(appleOAuthClient.revoke("apple-refresh")).willReturn(false);

        withdraw(login);

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void 카카오_사용자는_철회하지_않는다() throws Exception {
        String login = login("KAKAO", new OAuthUserInfo(UUID.randomUUID().toString(), "망고집사", null));

        withdraw(login);

        verify(appleOAuthClient, never()).revoke(any());
    }

    private String login(String provider, OAuthUserInfo userInfo) throws Exception {
        String code = provider.toLowerCase() + "-code";
        if (provider.equals("APPLE")) {
            given(appleOAuthClient.fetchUser(code, "https://app.example/callback")).willReturn(userInfo);
        } else {
            given(kakaoOAuthClient.fetchUser(code, "https://app.example/callback")).willReturn(userInfo);
        }
        return mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "provider": "%s", "code": "%s", "redirectUri": "https://app.example/callback" }
                                """.formatted(provider, code)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void withdraw(String loginResponse) throws Exception {
        String accessToken = JsonPath.read(loginResponse, "$.data.accessToken");
        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    private String storedToken(long userId) {
        return jdbcTemplate.queryForObject("select refresh_token from user_oauth_tokens where user_id = ?",
                String.class, userId);
    }

    private Integer countTokens(long userId) {
        return jdbcTemplate.queryForObject("select count(*) from user_oauth_tokens where user_id = ?",
                Integer.class, userId);
    }
}
