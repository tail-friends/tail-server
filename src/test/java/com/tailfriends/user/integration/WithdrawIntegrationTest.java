package com.tailfriends.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.RefreshTokenRepository;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class WithdrawIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void 탈퇴하면_계정과_개체_기록_리프레시_토큰이_지워지고_다른_사용자_데이터는_남는다() throws Exception {
        User user = saveUser();
        TokenResponse tokens = tokenService.issue(user);
        String token = "Bearer " + tokens.accessToken();
        long animalId = createAnimal(token);
        long logId = createLog(token, animalId);

        String otherToken = "Bearer " + tokenService.issue(saveUser()).accessToken();
        long otherAnimalId = createAnimal(otherToken);

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(animalRepository.findById(animalId)).isEmpty();
        assertThat(logRepository.findById(logId)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(RefreshToken.hash(tokens.refreshToken()))).isEmpty();
        assertThat(animalRepository.findById(otherAnimalId)).isPresent();

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }

    private long createAnimal(String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private long createLog(String token, long animalId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "clientId": "%s", "animalId": %d, "type": "WEIGHT", "date": "2026-10-18", "value": 52.3 }
                                """.formatted(UUID.randomUUID(), animalId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }
}
