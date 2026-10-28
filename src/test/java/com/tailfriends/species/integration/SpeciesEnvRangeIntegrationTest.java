package com.tailfriends.species.integration;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

/**
 * 기본 종의 온습도 범위를 채운 마이그레이션(V202610281438)을 확인한다.
 */
class SpeciesEnvRangeIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 기본_종_목록에_온습도_범위가_들어_있고_육지_거북만_비어_있다() throws Exception {
        mockMvc.perform(get("/api/v1/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.species[?(@.name == '레오파드 게코')].envRange.hot.min").value(34.0))
                .andExpect(jsonPath("$.data.species[?(@.name == '레오파드 게코')].envRange.hot.max").value(36.0))
                .andExpect(jsonPath("$.data.species[?(@.name == '레오파드 게코')].envRange.humidity.max").value(40))
                .andExpect(jsonPath("$.data.species[?(@.name == '크레스티드 게코')].envRange.hot.max").value(26.5))
                .andExpect(jsonPath("$.data.species[?(@.name == '킹 스네이크')].envRange.cool.max").value(26.0))
                .andExpect(jsonPath("$.data.species[?(@.name == '육지 거북')].envRange.hot").value(contains((Object) null)));
    }

    @Test
    void 기본_종도_온습도가_범위를_벗어나면_환경_경고가_나온다() throws Exception {
        String token = tokenOfNewUser();
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long animalId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "animalId": %d, "type": "ENV", "date": "2026-10-20", "hot": 30.0, "cool": 23.0, "humidity": 35 }
                                """.formatted(animalId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newAlerts[0].type").value("ENV_WARNING"))
                .andExpect(jsonPath("$.data.newAlerts[0].message").value("핫존 온도가 적정 범위(34~36℃)보다 낮아요"));
    }

    private String tokenOfNewUser() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
