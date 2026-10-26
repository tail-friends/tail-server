package com.tailfriends.schedule.integration;

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
import com.tailfriends.schedule.repository.ScheduleRepository;
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
 * 완료일은 오늘(KST) 이후가 안 되므로, 테스트 날짜는 모두 지난 날짜(2026년 9월)로 쓴다.
 */
class ScheduleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 일정을_만들고_완료하고_수정하고_삭제한다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);

        // 2026-09-06은 일요일. 월·목 반복이면 첫 일정일은 9-07(월)
        Integer id = createSchedule(token, """
                { "animalId": %d, "name": "칼슘 더스팅", "kind": "SUPPLEMENT",
                  "repeat": { "type": "WEEKDAYS", "days": ["THU", "MON"] }, "startDate": "2026-09-06" }
                """.formatted(animalId), "2026-09-07");

        mockMvc.perform(post("/api/v1/schedules/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-09-07\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastDoneDate").value("2026-09-07"))
                .andExpect(jsonPath("$.data.nextDate").value("2026-09-10"));

        mockMvc.perform(patch("/api/v1/schedules/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"repeat\": { \"type\": \"EVERY_N_DAYS\", \"n\": 3 }, \"endDate\": \"2026-09-30\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("칼슘 더스팅"))
                .andExpect(jsonPath("$.data.repeat.n").value(3))
                .andExpect(jsonPath("$.data.repeat.days").doesNotExist())
                .andExpect(jsonPath("$.data.nextDate").value("2026-09-10"))
                .andExpect(jsonPath("$.data.endDate").value("2026-09-30"));

        mockMvc.perform(delete("/api/v1/schedules/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(scheduleRepository.findById(id.longValue())).isEmpty();
    }

    @Test
    void 개체로_조회하면_그_개체와_공통_일정을_다음_일정일_순으로_끝난_일정은_뒤로_준다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        Integer kiwi = createAnimal(token);

        Integer ended = createSchedule(token, """
                { "animalId": %d, "name": "구충제", "kind": "MEDICATION",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "2026-09-01", "endDate": "2026-09-05" }
                """.formatted(mango), "2026-09-01");
        mockMvc.perform(post("/api/v1/schedules/" + ended + "/complete").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-09-01\" }"))
                .andExpect(jsonPath("$.data.nextDate").isEmpty());
        Integer lamp = createSchedule(token, """
                { "name": "UVB 램프 교체", "kind": "EQUIPMENT",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 180 }, "startDate": "2026-09-20" }
                """, "2026-09-20");
        Integer bath = createSchedule(token, """
                { "animalId": %d, "name": "온욕", "kind": "OTHER",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "2026-09-10" }
                """.formatted(mango), "2026-09-10");
        createSchedule(token, """
                { "animalId": %d, "name": "다른 개체 일정", "kind": "OTHER",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "2026-09-01" }
                """.formatted(kiwi), "2026-09-01");

        mockMvc.perform(get("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", mango.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedules[*].id", contains(bath, lamp, ended)))
                .andExpect(jsonPath("$.data.schedules[1].animalId").isEmpty());
        mockMvc.perform(get("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.schedules", hasSize(4)));
    }

    @Test
    void 개체를_지우면_그_개체의_일정도_지워진다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        Integer id = createSchedule(token, """
                { "animalId": %d, "name": "온욕", "kind": "OTHER",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "2026-09-10" }
                """.formatted(animalId), "2026-09-10");

        mockMvc.perform(delete("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(scheduleRepository.findById(id.longValue())).isEmpty();
    }

    @Test
    void 다른_사용자의_일정과_개체는_404다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        Integer animalId = createAnimal(owner);
        Integer id = createSchedule(owner, """
                { "animalId": %d, "name": "온욕", "kind": "OTHER",
                  "repeat": { "type": "EVERY_N_DAYS", "n": 7 }, "startDate": "2026-09-10" }
                """.formatted(animalId), "2026-09-10");

        mockMvc.perform(post("/api/v1/schedules/" + id + "/complete").header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-09-10\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, other)
                        .param("animalId", animalId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    private Integer createSchedule(String token, String body, String expectedNextDate) throws Exception {
        String response = mockMvc.perform(post("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nextDate").value(expectedNextDate))
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
