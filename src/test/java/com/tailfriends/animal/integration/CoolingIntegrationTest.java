package com.tailfriends.animal.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

class CoolingIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 쿨링_중이면_급여_지남과_거식_환경_경고를_멈추고_끄면_돌아온다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        // 급여 주기 4일인데 10일 전에 마지막으로 먹었다: 쿨링이 아니면 OVERDUE
        writeLog(token, animalId, "\"type\": \"FEED\"", TODAY.minusDays(10));
        // 안 먹음 2번. 한 번 더 안 먹으면 평소엔 거식 경고(3번 연속)
        writeLog(token, animalId, "\"type\": \"FEED\", \"refused\": true", TODAY.minusDays(9));
        writeLog(token, animalId, "\"type\": \"FEED\", \"refused\": true", TODAY.minusDays(8));

        changeCooling(token, animalId, "{ \"startDate\": \"%s\", \"endDate\": null }".formatted(TODAY.minusDays(20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cooling.startDate").value(TODAY.minusDays(20).toString()))
                .andExpect(jsonPath("$.data.cooling.endDate").isEmpty());
        // 쿨링 중에 3번째 안 먹음, 온도를 낮춘 온습도 기록 (레오파드 게코 핫존 34~36℃)
        writeLog(token, animalId, "\"type\": \"FEED\", \"refused\": true", TODAY.minusDays(1))
                .andExpect(jsonPath("$.data.newAlerts", hasSize(0)));
        writeLog(token, animalId, "\"type\": \"ENV\", \"hot\": 20.0, \"cool\": 15.0", TODAY)
                .andExpect(jsonPath("$.data.newAlerts", hasSize(0)));

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.feed.status").value("COOLING"))
                .andExpect(jsonPath("$.data.mood").value("SLEEPING"))
                .andExpect(jsonPath("$.data.cooling.startDate").value(TODAY.minusDays(20).toString()))
                .andExpect(jsonPath("$.data.alerts", hasSize(0)));
        mockMvc.perform(get("/api/v1/today").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.feeds[?(@.animalId == " + animalId + ")]", hasSize(0)));

        changeCooling(token, animalId, "{ \"startDate\": null, \"endDate\": null }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cooling").isEmpty());

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.feed.status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.cooling").isEmpty());
    }

    @Test
    void 종료일이_지난_쿨링은_정해_둔_값만_남고_급여_계산은_평소대로다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        writeLog(token, animalId, "\"type\": \"FEED\"", TODAY.minusDays(10));

        changeCooling(token, animalId, "{ \"startDate\": \"%s\", \"endDate\": \"%s\" }"
                .formatted(TODAY.minusDays(30), TODAY.minusDays(1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.feed.status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.cooling.endDate").value(TODAY.minusDays(1).toString()));
    }

    @Test
    void 종료일이_시작일보다_빠르면_400이고_남의_개체는_404() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        long animalId = createAnimal(owner);

        changeCooling(owner, animalId, "{ \"startDate\": \"2026-12-01\", \"endDate\": \"2026-11-30\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        changeCooling(other, animalId, "{ \"startDate\": \"2026-12-01\", \"endDate\": null }")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    private ResultActions changeCooling(String token, long animalId, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/animals/" + animalId + "/cooling").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions writeLog(String token, long animalId, String typeAndFields, LocalDate date)
            throws Exception {
        return mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"animalId\": %d, \"date\": \"%s\", %s }".formatted(animalId, date, typeAndFields)));
    }

    private long createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
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
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
