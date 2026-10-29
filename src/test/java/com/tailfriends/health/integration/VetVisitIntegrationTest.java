package com.tailfriends.health.integration;

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

class VetVisitIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 진료_기록을_적고_투약_일정을_오늘부터_만들고_지워도_일정은_남는다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);

        String body = createVisit(token, """
                { "animalId": %d, "hospital": "제주 특수동물병원", "visitDate": "%s", "symptom": "부분 탈피",
                  "diagnosis": "탈피 부전", "prescription": "온욕 주 2회", "nextVisitDate": "%s",
                  "createMedicationSchedule": { "name": "온욕",
                    "repeat": { "type": "EVERY_N_DAYS", "n": 3 }, "endDate": "%s" } }
                """.formatted(animalId, TODAY.minusDays(5), TODAY.plusDays(9), TODAY.plusDays(30)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scheduleId").isNumber())
                .andReturn().getResponse().getContentAsString();
        long visitId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        long scheduleId = ((Number) JsonPath.read(body, "$.data.scheduleId")).longValue();

        mockMvc.perform(get("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.schedules[?(@.id == " + scheduleId + ")].kind").value("MEDICATION"))
                .andExpect(jsonPath("$.data.schedules[?(@.id == " + scheduleId + ")].startDate")
                        .value(TODAY.toString()));

        mockMvc.perform(patch("/api/v1/health/vet-visits/" + visitId).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"diagnosis\": \"탈피 부전 (호전)\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diagnosis").value("탈피 부전 (호전)"))
                .andExpect(jsonPath("$.data.hospital").value("제주 특수동물병원"));

        mockMvc.perform(delete("/api/v1/health/vet-visits/" + visitId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        visits(token, animalId).andExpect(jsonPath("$.data.visits", hasSize(0)));
        mockMvc.perform(get("/api/v1/schedules").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", String.valueOf(animalId)))
                .andExpect(jsonPath("$.data.schedules[?(@.id == " + scheduleId + ")]", hasSize(1)));
    }

    @Test
    void 목록은_방문일_최신순이다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        createVisit(token, visitBody(animalId, TODAY.minusDays(30), "첫 진료"));
        createVisit(token, visitBody(animalId, TODAY, "오늘 진료"));
        createVisit(token, visitBody(animalId, TODAY.minusDays(10), "중간 진료"));

        visits(token, animalId)
                .andExpect(jsonPath("$.data.visits", hasSize(3)))
                .andExpect(jsonPath("$.data.visits[0].hospital").value("오늘 진료"))
                .andExpect(jsonPath("$.data.visits[2].hospital").value("첫 진료"));
    }

    @Test
    void 투약_일정이_잘못되면_진료_기록도_저장하지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);

        createVisit(token, """
                { "animalId": %d, "visitDate": "%s",
                  "createMedicationSchedule": { "name": "온욕", "repeat": { "type": "WEEKDAYS" } } }
                """.formatted(animalId, TODAY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        visits(token, animalId).andExpect(jsonPath("$.data.visits", hasSize(0)));
    }

    @Test
    void 방문일이_미래거나_다음_방문일이_빠르면_400이고_남의_기록은_404() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        long animalId = createAnimal(owner);

        createVisit(owner, visitBody(animalId, TODAY.plusDays(1), "병원"))
                .andExpect(status().isBadRequest());
        createVisit(owner, """
                { "animalId": %d, "visitDate": "%s", "nextVisitDate": "%s" }
                """.formatted(animalId, TODAY, TODAY))
                .andExpect(status().isBadRequest());

        String body = createVisit(owner, visitBody(animalId, TODAY, "병원"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long visitId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        mockMvc.perform(delete("/api/v1/health/vet-visits/" + visitId).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
        visits(other, animalId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
        createVisit(other, visitBody(animalId, TODAY, "남의 개체"))
                .andExpect(status().isNotFound());
    }

    private static String visitBody(long animalId, LocalDate visitDate, String hospital) {
        return "{ \"animalId\": %d, \"visitDate\": \"%s\", \"hospital\": \"%s\" }"
                .formatted(animalId, visitDate, hospital);
    }

    private ResultActions createVisit(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/health/vet-visits").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions visits(String token, long animalId) throws Exception {
        return mockMvc.perform(get("/api/v1/health/vet-visits").header(HttpHeaders.AUTHORIZATION, token)
                .param("animalId", String.valueOf(animalId)));
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
