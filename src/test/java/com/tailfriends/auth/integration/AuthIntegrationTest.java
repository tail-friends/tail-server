package com.tailfriends.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void 재발급하면_새_리프레시_토큰으로_교체되고_쓴_토큰은_다시_쓸_수_없다() throws Exception {
        TokenResponse tokens = tokenService.issue(saveUser());

        String body = reissue(tokens.refreshToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String newRefreshToken = JsonPath.read(body, "$.data.refreshToken");

        assertThat(refreshTokenRepository.findByTokenHash(RefreshToken.hash(tokens.refreshToken()))).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(RefreshToken.hash(newRefreshToken))).isPresent();
        reissue(tokens.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 로그아웃하면_리프레시_토큰이_폐기되어_재발급할_수_없다() throws Exception {
        TokenResponse tokens = tokenService.issue(saveUser());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());

        reissue(tokens.refreshToken())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 남의_리프레시_토큰으로_로그아웃해도_그_토큰은_지워지지_않는다() throws Exception {
        TokenResponse mine = tokenService.issue(saveUser());
        TokenResponse others = tokenService.issue(saveUser());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mine.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + others.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByTokenHash(RefreshToken.hash(others.refreshToken()))).isPresent();
    }

    private ResultActions reissue(String refreshToken) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }
}
