package com.tailfriends.log.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
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

class LogIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 기록을_작성하고_조회하고_수정하고_삭제한다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);

        Integer logId = createLog(token, """
                { "animalId": %d, "type": "FEED", "date": "2026-10-14",
                  "food": "밀웜", "count": 5, "dusted": true, "value": 99.9, "memo": "" }
                """.formatted(animalId));

        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", animalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs", hasSize(1)))
                .andExpect(jsonPath("$.data.logs[0].food").value("밀웜"))
                .andExpect(jsonPath("$.data.logs[0].refused").value(false))
                .andExpect(jsonPath("$.data.logs[0].value").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());

        mockMvc.perform(patch("/api/v1/logs/" + logId).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"count\": 4, \"memo\": \"1마리 남김\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(4))
                .andExpect(jsonPath("$.data.food").value("밀웜"))
                .andExpect(jsonPath("$.data.memo").value("1마리 남김"));

        mockMvc.perform(delete("/api/v1/logs/" + logId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", animalId.toString()))
                .andExpect(jsonPath("$.data.logs", hasSize(0)));
    }

    @Test
    void 목록은_기록일_최신순이고_커서로_다음_페이지를_가져온다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        Integer oct10 = createWeightLog(token, animalId, "2026-10-10");
        Integer oct15 = createWeightLog(token, animalId, "2026-10-15");
        Integer oct12 = createWeightLog(token, animalId, "2026-10-12");
        createLog(token, """
                { "animalId": %d, "type": "NOTE", "date": "2026-10-16", "memo": "활발함" }
                """.formatted(animalId));

        String first = mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", animalId.toString()).param("type", "WEIGHT").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs[*].id").value(contains(oct15, oct12)))
                .andExpect(jsonPath("$.data.nextCursor").value(oct12))
                .andReturn().getResponse().getContentAsString();
        Integer cursor = JsonPath.read(first, "$.data.nextCursor");

        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", animalId.toString()).param("type", "WEIGHT")
                        .param("size", "2").param("cursor", cursor.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs[*].id").value(contains(oct10)))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
    }

    @Test
    void 같은_clientId로_다시_보내면_409와_기존_기록_ID를_돌려준다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        String body = """
                { "clientId": "%s", "animalId": %d, "type": "WEIGHT", "date": "2026-10-18", "value": 52.3 }
                """.formatted(UUID.randomUUID(), animalId);
        Integer logId = createLog(token, body);

        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CLIENT_ID"))
                .andExpect(jsonPath("$.data.id").value(logId));
    }

    @Test
    void 남의_개체와_기록에는_접근할_수_없다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        Integer animalId = createAnimal(owner);
        Integer logId = createWeightLog(owner, animalId, "2026-10-18");

        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, other)
                        .param("animalId", animalId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "animalId": %d, "type": "NOTE", "date": "2026-10-18", "memo": "남의 개체" }
                                """.formatted(animalId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/logs/" + logId).header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"memo\": \"뺏기\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LOG_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/logs/" + logId).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound());
    }

    @Test
    void 개체를_삭제하면_기록도_함께_삭제된다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        Integer logId = createWeightLog(token, animalId, "2026-10-18");

        mockMvc.perform(delete("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(logRepository.findById(logId.longValue())).isEmpty();
    }

    private Integer createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private Integer createWeightLog(String token, Integer animalId, String date) throws Exception {
        return createLog(token, """
                { "animalId": %d, "type": "WEIGHT", "date": "%s", "value": 52.3 }
                """.formatted(animalId, date));
    }

    private Integer createLog(String token, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
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
