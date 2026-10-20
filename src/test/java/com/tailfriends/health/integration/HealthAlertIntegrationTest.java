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
import com.tailfriends.global.util.KstDate;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class HealthAlertIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 안_먹음이_3번_연속이면_거식_경고가_생기고_한_번만_만든다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        refuse(token, animalId, TODAY.minusDays(8)).andExpect(jsonPath("$.data.newAlerts", hasSize(0)));
        refuse(token, animalId, TODAY.minusDays(4)).andExpect(jsonPath("$.data.newAlerts", hasSize(0)));

        refuse(token, animalId, TODAY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newAlerts", hasSize(1)))
                .andExpect(jsonPath("$.data.newAlerts[0].type").value("REFUSAL_ALERT"))
                .andExpect(jsonPath("$.data.newAlerts[0].animalId").value(animalId));
        refuse(token, animalId, TODAY).andExpect(jsonPath("$.data.newAlerts", hasSize(0)));

        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alerts", hasSize(1)));
        mockMvc.perform(get("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.animals[0].alertCount").value(1));
    }

    @Test
    void 확인_처리한_경고는_열린_경고에서_빠진다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        weigh(token, animalId, TODAY.minusDays(20), "57.0");
        String body = weigh(token, animalId, TODAY, "50.0")
                .andExpect(jsonPath("$.data.newAlerts[0].type").value("WEIGHT_LOSS_ALERT"))
                .andExpect(jsonPath("$.data.newAlerts[0].message").value("최근 30일 동안 체중이 12% 줄었어요"))
                .andReturn().getResponse().getContentAsString();
        Integer alertId = JsonPath.read(body, "$.data.newAlerts[0].id");

        mockMvc.perform(patch("/api/v1/health/alerts/" + alertId).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"ACKNOWLEDGED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));

        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", animalId.toString()).param("status", "OPEN"))
                .andExpect(jsonPath("$.data.alerts", hasSize(0)));
        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.alerts", hasSize(0)));
    }

    @Test
    void 묽은_변이_2번_연속이면_배변_경고가_개체_상세에_보인다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        clean(token, animalId, TODAY.minusDays(1), "LOOSE");
        clean(token, animalId, TODAY, "NONE");
        clean(token, animalId, TODAY, "LOOSE").andExpect(jsonPath("$.data.newAlerts[0].type").value("STOOL_ALERT"));

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)))
                .andExpect(jsonPath("$.data.alerts[0].type").value("STOOL_ALERT"));
    }

    @Test
    void 남의_경고는_조회하거나_확인할_수_없다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        Integer animalId = createAnimal(owner);
        clean(owner, animalId, TODAY.minusDays(1), "LOOSE");
        String body = clean(owner, animalId, TODAY, "LOOSE").andReturn().getResponse().getContentAsString();
        Integer alertId = JsonPath.read(body, "$.data.newAlerts[0].id");

        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(jsonPath("$.data.alerts", hasSize(0)));
        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, other)
                        .param("animalId", animalId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/health/alerts/" + alertId).header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"status\": \"ACKNOWLEDGED\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ALERT_NOT_FOUND"));
    }

    private ResultActions refuse(String token, Integer animalId, LocalDate date) throws Exception {
        return createLog(token, """
                { "animalId": %d, "type": "FEED", "date": "%s", "refused": true }
                """.formatted(animalId, date));
    }

    private ResultActions weigh(String token, Integer animalId, LocalDate date, String grams) throws Exception {
        return createLog(token, """
                { "animalId": %d, "type": "WEIGHT", "date": "%s", "value": %s }
                """.formatted(animalId, date, grams));
    }

    private ResultActions clean(String token, Integer animalId, LocalDate date, String stool) throws Exception {
        return createLog(token, """
                { "animalId": %d, "type": "CLEAN", "date": "%s", "stool": "%s" }
                """.formatted(animalId, date, stool));
    }

    private ResultActions createLog(String token, String content) throws Exception {
        return mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isCreated());
    }

    private Integer createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
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
