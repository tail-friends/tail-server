package com.tailfriends.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.security.JwtProvider;
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

class DevLoginDisabledIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 설정을_켜지_않으면_테스트_로그인은_없다() throws Exception {
        mockMvc.perform(post("/api/v1/dev/login").contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"x\" }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 설정을_켜지_않으면_로그인해도_샘플_데이터_API는_없다() throws Exception {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        String token = "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());

        mockMvc.perform(post("/api/v1/dev/sample-data").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }
}
