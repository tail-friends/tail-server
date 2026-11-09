package com.tailfriends.photo.integration;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.photo.domain.AiKind;
import com.tailfriends.photo.domain.AiUsage;
import com.tailfriends.photo.repository.AiUsageRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

class AiUsageIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiUsageRepository aiUsageRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 캐릭터_사육장_건강_체크의_성공한_분석을_합쳐_세고_실패와_남의_분석은_세지_않는다() throws Exception {
        User user = saveUser();
        User other = saveUser();
        aiUsageRepository.save(AiUsage.of(user, AiKind.CHARACTER, true));
        aiUsageRepository.save(AiUsage.of(user, AiKind.ENCLOSURE, true));
        aiUsageRepository.save(AiUsage.of(user, AiKind.HEALTH_CHECK, true));
        aiUsageRepository.save(AiUsage.of(user, AiKind.CHARACTER, false));
        aiUsageRepository.save(AiUsage.of(other, AiKind.CHARACTER, true));

        mockMvc.perform(get("/api/v1/photos/ai-usage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "
                                + jwtProvider.createAccessToken(user.getId(), user.getRole())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(5))
                .andExpect(jsonPath("$.data.used").value(3))
                .andExpect(jsonPath("$.data.resetsAt").value(endsWith("T15:00:00Z")));
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }
}
