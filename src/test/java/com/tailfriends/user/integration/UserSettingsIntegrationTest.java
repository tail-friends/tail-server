package com.tailfriends.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import com.tailfriends.user.repository.UserSettingsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class UserSettingsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 처음엔_기본값이고_바꾼_값과_동의가_함께_저장된다() throws Exception {
        User user = saveUser();
        String token = token(user);

        mockMvc.perform(get("/api/v1/users/me/settings").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifyTime").value("09:00"))
                .andExpect(jsonPath("$.data.tempUnit").value("C"))
                .andExpect(jsonPath("$.data.keepOriginalDefault").value(false))
                .andExpect(jsonPath("$.data.alertRules.refusalCount").value(3))
                .andExpect(jsonPath("$.data.pushEnabled").value(false));
        assertThat(userSettingsRepository.findById(user.getId())).isEmpty();

        mockMvc.perform(patch("/api/v1/users/me/settings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "notifyTime": "08:30", "tempUnit": "F", "keepOriginalDefault": true,
                                  "alertRules": { "refusalCount": 5 }, "pushEnabled": true }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me/settings").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.notifyTime").value("08:30"))
                .andExpect(jsonPath("$.data.tempUnit").value("F"))
                .andExpect(jsonPath("$.data.keepOriginalDefault").value(true))
                .andExpect(jsonPath("$.data.alertRules.refusalCount").value(5))
                .andExpect(jsonPath("$.data.alertRules.weightLossPercent").value(10))
                .andExpect(jsonPath("$.data.pushEnabled").value(true));
        mockMvc.perform(get("/api/v1/auth/consents").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.keepOriginal").value(true))
                .andExpect(jsonPath("$.data.notification").value(true));
    }

    @Test
    void 탈퇴하면_설정도_함께_지워진다() throws Exception {
        User user = saveUser();
        String token = token(user);
        mockMvc.perform(patch("/api/v1/users/me/settings").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"tempUnit\": \"F\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(userSettingsRepository.findById(user.getId())).isEmpty();
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }

    private String token(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
