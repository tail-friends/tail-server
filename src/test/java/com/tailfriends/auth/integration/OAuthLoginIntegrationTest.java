package com.tailfriends.auth.integration;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.auth.client.KakaoOAuthClient;
import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 카카오 서버 호출만 가짜로 바꾸고, 가입·토큰 발급·발급한 토큰으로 API 호출까지 실제 DB로 확인한다.
 */
class OAuthLoginIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        given(kakaoOAuthClient.provider()).willReturn(Provider.KAKAO);
    }

    @Test
    void 처음_로그인하면_가입되고_두_번째부터는_같은_사용자로_로그인한다() throws Exception {
        String kakaoId = UUID.randomUUID().toString();
        given(kakaoOAuthClient.fetchUser("auth-code", "https://app.example/callback"))
                .willReturn(new OAuthUserInfo(kakaoId, "망고집사", "keeper@example.com"));

        String first = login()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.user.nickname").value("망고집사"))
                .andReturn().getResponse().getContentAsString();
        Integer userId = JsonPath.read(first, "$.data.user.id");
        String accessToken = JsonPath.read(first, "$.data.accessToken");

        login()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(jsonPath("$.data.user.id").value(userId));

        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void 애플_로그인은_아직_지원하지_않아_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "provider": "APPLE", "code": "auth-code", "redirectUri": "https://app.example/callback" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/oauth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "provider": "KAKAO", "code": "auth-code", "redirectUri": "https://app.example/callback" }
                        """));
    }
}
