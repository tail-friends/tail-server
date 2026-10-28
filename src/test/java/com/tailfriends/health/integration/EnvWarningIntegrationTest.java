package com.tailfriends.health.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import com.tailfriends.species.repository.SpeciesRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 기본 10종은 온습도 범위가 비어 있어서, 범위를 채운 종을 따로 만들어 확인한다.
 * 종 목록 테스트가 기본 10종만 있다고 보므로 만든 종은 테스트가 끝나면 지운다.
 */
class EnvWarningIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpeciesRepository speciesRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> createdSpeciesIds = new ArrayList<>();

    // 사용자를 지우면 개체·기록·경고는 DB의 ON DELETE CASCADE로 함께 지워진다. 그 뒤에 종을 지운다
    @AfterEach
    void cleanUp() {
        userRepository.deleteAllById(createdUserIds);
        speciesRepository.deleteAllById(createdSpeciesIds);
    }

    @Test
    void 온습도_기록이_범위를_벗어나면_환경_경고를_돌려주고_목록에도_남긴다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token, speciesWithRanges().getId());

        writeEnv(token, animalId, "29.5", "24.0", 35)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newAlerts[0].type").value("ENV_WARNING"))
                .andExpect(jsonPath("$.data.newAlerts[0].message").value("핫존 온도가 적정 범위(31~35℃)보다 낮아요"));

        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)))
                .andExpect(jsonPath("$.data.alerts[0].type").value("ENV_WARNING"))
                .andExpect(jsonPath("$.data.alerts[0].status").value("OPEN"));
    }

    @Test
    void 범위_안이면_경고가_없고_수정해서_벗어나면_그때_만든다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token, speciesWithRanges().getId());

        String body = writeEnv(token, animalId, "32.0", "24.0", 35)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newAlerts", hasSize(0)))
                .andReturn().getResponse().getContentAsString();
        long logId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        mockMvc.perform(patch("/api/v1/logs/" + logId).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"hot\": 37.0, \"humidity\": 60 }"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)))
                .andExpect(jsonPath("$.data.alerts[0].message").value("핫존 온도와 습도가 적정 범위를 벗어났어요"));
    }

    @Test
    void 종에_범위가_없으면_경고를_만들지_않는다() throws Exception {
        String token = tokenOfNewUser();
        // 육지 거북은 종 원본 표에도 온습도 범위가 없다
        long animalId = createAnimal(token, speciesRepository.findByName("육지 거북").orElseThrow().getId());

        writeEnv(token, animalId, "10.0", "5.0", 100)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newAlerts", hasSize(0)));
    }

    private ResultActions writeEnv(String token, long animalId, String hot, String cool, int humidity)
            throws Exception {
        return mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "animalId": %d, "type": "ENV", "date": "2026-10-20", "hot": %s, "cool": %s, "humidity": %d }
                        """.formatted(animalId, hot, cool, humidity)));
    }

    private Species speciesWithRanges() {
        Species species = speciesRepository.save(Species.builder()
                .name("온습도 테스트 " + UUID.randomUUID().toString().substring(0, 8))
                .shape(Shape.GECKO)
                .feedIntervalDays(4)
                .hotMin(new BigDecimal("31.0"))
                .hotMax(new BigDecimal("35.0"))
                .coolMin(new BigDecimal("22.0"))
                .coolMax(new BigDecimal("26.0"))
                .humidityMin(30)
                .humidityMax(40)
                .defaultHome(Home.DESERT)
                .build());
        createdSpeciesIds.add(species.getId());
        return species;
    }

    private long createAnimal(String token, long speciesId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": %d, \"feedIntervalDays\": 4 }".formatted(speciesId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private String tokenOfNewUser() {
        User user = userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
        createdUserIds.add(user.getId());
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
