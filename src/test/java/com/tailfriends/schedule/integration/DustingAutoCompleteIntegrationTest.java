package com.tailfriends.schedule.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.schedule.domain.Schedule;
import com.tailfriends.schedule.repository.ScheduleRepository;
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

/**
 * 기록일은 오늘(KST) 이후가 안 되므로, 테스트 날짜는 모두 지난 날짜(2026년 9월)로 쓴다.
 */
class DustingAutoCompleteIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate FED = LocalDate.of(2026, 9, 7);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 더스팅한_급여_기록은_그_개체와_공통_더스팅_일정_중_기록일까지_해야_하는_것만_완료한다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        Integer kiwi = createAnimal(token);
        Integer overdue = createSchedule(token, mango, "DUSTING", 3, "2026-09-05");
        Integer common = createSchedule(token, null, "DUSTING", 7, "2026-09-07");
        Integer otherAnimal = createSchedule(token, kiwi, "DUSTING", 3, "2026-09-05");
        Integer supplement = createSchedule(token, mango, "SUPPLEMENT", 3, "2026-09-05");
        Integer later = createSchedule(token, mango, "DUSTING", 3, "2026-09-10");

        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "animalId": %d, "type": "FEED", "date": "2026-09-07",
                                  "food": "귀뚜라미", "count": 6, "dusted": true }
                                """.formatted(mango)))
                .andExpect(status().isCreated());

        assertDone(overdue, LocalDate.of(2026, 9, 10));
        assertDone(common, LocalDate.of(2026, 9, 14));
        assertNotDone(otherAnimal);
        assertNotDone(supplement);
        assertNotDone(later);
    }

    @Test
    void 일괄_기록에서_더스팅하지_않은_급여는_완료하지_않고_공통_일정은_한_번만_완료한다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        Integer kiwi = createAnimal(token);
        Integer common = createSchedule(token, null, "DUSTING", 7, "2026-09-07");
        Integer kiwiDusting = createSchedule(token, kiwi, "DUSTING", 3, "2026-09-07");

        mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "FEED", "date": "2026-09-07",
                                  "items": [ { "animalId": %d, "food": "귀뚜라미", "count": 6, "dusted": true },
                                             { "animalId": %d, "food": "귀뚜라미", "count": 4, "dusted": false } ] }
                                """.formatted(mango, kiwi)))
                .andExpect(status().isCreated());

        assertDone(common, LocalDate.of(2026, 9, 14));
        assertNotDone(kiwiDusting);
    }

    private void assertDone(Integer scheduleId, LocalDate nextDate) {
        Schedule schedule = scheduleRepository.findById(scheduleId.longValue()).orElseThrow();
        assertThat(schedule.getLastDoneDate()).isEqualTo(FED);
        assertThat(schedule.getNextDate()).isEqualTo(nextDate);
    }

    private void assertNotDone(Integer scheduleId) {
        assertThat(scheduleRepository.findById(scheduleId.longValue()).orElseThrow().getLastDoneDate()).isNull();
    }

    private Integer createSchedule(String token, Integer animalId, String kind, int n, String startDate)
            throws Exception {
        String response = mockMvc.perform(post("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "animalId": %s, "name": "칼슘 더스팅", "kind": "%s",
                                  "repeat": { "type": "EVERY_N_DAYS", "n": %d }, "startDate": "%s" }
                                """.formatted(animalId, kind, n, startDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nextDate").value(startDate))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id");
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
