package com.tailfriends.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Consent;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.ConsentRepository;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class ConsentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 처음엔_모두_false이고_저장한_뒤에는_바뀐_항목만_이력으로_쌓인다() throws Exception {
        User user = saveUser();
        String token = "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());

        mockMvc.perform(get("/api/v1/auth/consents").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacy").value(false))
                .andExpect(jsonPath("$.data.photoAnalysis").value(false));

        save(token, true, true, false, true).andExpect(status().isOk());
        save(token, true, true, true, false).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/consents").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacy").value(true))
                .andExpect(jsonPath("$.data.photoAnalysis").value(true))
                .andExpect(jsonPath("$.data.keepOriginal").value(true))
                .andExpect(jsonPath("$.data.notification").value(false));
        assertThat(consentRepository.findAllByUserIdOrderByIdDesc(user.getId()))
                .extracting(Consent::getType, Consent::isAgreed)
                .containsExactly(
                        tuple(ConsentType.NOTIFICATION, false),
                        tuple(ConsentType.KEEP_ORIGINAL, true),
                        tuple(ConsentType.NOTIFICATION, true),
                        tuple(ConsentType.KEEP_ORIGINAL, false),
                        tuple(ConsentType.PHOTO_ANALYSIS, true),
                        tuple(ConsentType.PRIVACY, true));
    }

    @Test
    void 필수_항목이_false면_400이고_저장하지_않는다() throws Exception {
        User user = saveUser();
        String token = "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());

        save(token, false, true, false, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        assertThat(consentRepository.findAllByUserIdOrderByIdDesc(user.getId())).isEmpty();
    }

    private ResultActions save(String token, boolean privacy, boolean photoAnalysis,
                               boolean keepOriginal, boolean notification) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/consents").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "privacy": %s, "photoAnalysis": %s, "keepOriginal": %s, "notification": %s }
                        """.formatted(privacy, photoAnalysis, keepOriginal, notification)));
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }
}
