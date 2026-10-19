package com.tailfriends.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

class TokenIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void 사용자를_저장하면_기본_권한과_포인트가_들어간다() {
        User user = saveUser();

        User found = userRepository.findByProviderAndProviderId(Provider.KAKAO, user.getProviderId()).orElseThrow();
        assertThat(found.getRole()).isEqualTo(Role.KEEPER);
        assertThat(found.getPoints()).isZero();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 발급한_리프레시_토큰은_해시로_조회된다() {
        TokenResponse tokens = tokenService.issue(saveUser());

        RefreshToken saved = refreshTokenRepository.findByTokenHash(RefreshToken.hash(tokens.refreshToken())).orElseThrow();
        assertThat(saved.getTokenHash()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void 액세스_토큰이_있으면_인증이_필요한_경로를_통과한다() throws Exception {
        TokenResponse tokens = tokenService.issue(saveUser());

        // 아직 인증이 필요한 API가 없어 없는 경로로 확인한다. 보안 필터를 통과하면 404가 된다.
        mockMvc.perform(get("/api/v1/not-exists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void 액세스_토큰이_없으면_401을_돌려준다() throws Exception {
        mockMvc.perform(get("/api/v1/not-exists"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }
}
