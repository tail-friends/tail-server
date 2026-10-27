package com.tailfriends.today.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 서버 시계(오늘, KST)를 그대로 쓰므로 날짜는 모두 오늘 기준으로 만든다.
 */
class TodayIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private Clock clock;

    @Test
    void 급여_일정_경고_오늘_완료를_한_번에_돌려준다() throws Exception {
        LocalDate today = KstDate.today(clock);
        String token = tokenOfNewUser();
        // 급여 주기 4일
        Integer due = createAnimal(token, "망고");
        Integer overdue = createAnimal(token, "초코");
        Integer upcoming = createAnimal(token, "키위");

        createLog(token, "{ \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", \"food\": \"귀뚜라미\", \"count\": 6 }"
                .formatted(due, today.minusDays(4)));
        createLog(token, "{ \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", \"food\": \"해동 마우스\", \"count\": 1 }"
                .formatted(overdue, today.minusDays(10)));
        for (int day = 1; day <= 3; day++) {
            createLog(token, "{ \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", \"refused\": true }"
                    .formatted(overdue, today.minusDays(day)));
        }
        Integer todayLog = createLog(token, "{ \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", \"food\": \"밀웜\", \"count\": 3 }"
                .formatted(upcoming, today));

        Integer missed = createSchedule(token, """
                { "animalId": %d, "name": "칼슘 더스팅", "kind": "SUPPLEMENT",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "%s" }
                """.formatted(due, today.minusDays(3)));
        createSchedule(token, """
                { "name": "바닥재 교체", "kind": "EQUIPMENT",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 30 }, "startDate": "%s" }
                """.formatted(today.plusDays(1)));
        Integer lamp = createSchedule(token, """
                { "name": "UVB 램프 점검", "kind": "EQUIPMENT",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "%s" }
                """.formatted(today.minusDays(1)));
        mockMvc.perform(post("/api/v1/schedules/" + lamp + "/complete").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"%s\" }".formatted(today)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/today").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value(today.toString()))
                .andExpect(jsonPath("$.data.feeds[*].animalId", contains(overdue, due)))
                .andExpect(jsonPath("$.data.feeds[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$.data.feeds[0].overdueDays").value(6))
                .andExpect(jsonPath("$.data.feeds[0].suggested.food").value("해동 마우스"))
                .andExpect(jsonPath("$.data.feeds[1].status").value("DUE"))
                .andExpect(jsonPath("$.data.feeds[1].suggested.count").value(6))
                .andExpect(jsonPath("$.data.feeds[1].thumbnailUrl").isEmpty())
                .andExpect(jsonPath("$.data.schedules[*].scheduleId", contains(missed)))
                .andExpect(jsonPath("$.data.schedules[0].nextDate").value(today.minusDays(3).toString()))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)))
                .andExpect(jsonPath("$.data.alerts[0].animalId").value(overdue))
                .andExpect(jsonPath("$.data.alerts[0].type").value("REFUSAL_ALERT"))
                .andExpect(jsonPath("$.data.done", hasSize(2)))
                .andExpect(jsonPath("$.data.done[0].type").value("FEED"))
                .andExpect(jsonPath("$.data.done[0].logId").value(todayLog))
                .andExpect(jsonPath("$.data.done[1].type").value("SCHEDULE"))
                .andExpect(jsonPath("$.data.done[1].scheduleId").value(lamp));
    }

    @Test
    void 다른_사용자의_할_일은_보이지_않는다() throws Exception {
        LocalDate today = KstDate.today(clock);
        String owner = tokenOfNewUser();
        Integer animalId = createAnimal(owner, "망고");
        createLog(owner, "{ \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", \"food\": \"귀뚜라미\" }"
                .formatted(animalId, today.minusDays(4)));

        mockMvc.perform(get("/api/v1/today").header(HttpHeaders.AUTHORIZATION, tokenOfNewUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeds", hasSize(0)))
                .andExpect(jsonPath("$.data.schedules", hasSize(0)))
                .andExpect(jsonPath("$.data.alerts", hasSize(0)))
                .andExpect(jsonPath("$.data.done", hasSize(0)));
    }

    @Test
    void 오늘이_아닌_날짜는_400() throws Exception {
        LocalDate today = KstDate.today(clock);

        mockMvc.perform(get("/api/v1/today").header(HttpHeaders.AUTHORIZATION, tokenOfNewUser())
                        .param("date", today.minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    private Integer createAnimal(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"%s\", \"speciesId\": 1, \"feedIntervalDays\": 4 }".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private Integer createLog(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
    }

    private Integer createSchedule(String token, String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
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
