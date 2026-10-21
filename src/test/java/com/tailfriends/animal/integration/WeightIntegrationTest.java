package com.tailfriends.animal.integration;

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
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class WeightIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 기본은_최근_1년의_체중만_날짜순으로_돌려준다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        log(token, animalId, "WEIGHT", TODAY.minusDays(3), "\"value\": 57.8");
        log(token, animalId, "WEIGHT", TODAY.minusYears(1).minusDays(1), "\"value\": 30.0");
        log(token, animalId, "WEIGHT", TODAY.minusDays(90), "\"value\": 48.0");
        log(token, animalId, "NOTE", TODAY, "\"memo\": \"체중 아님\"");

        mockMvc.perform(get("/api/v1/animals/" + animalId + "/weights").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points", hasSize(2)))
                .andExpect(jsonPath("$.data.points[*].date",
                        contains(TODAY.minusDays(90).toString(), TODAY.minusDays(3).toString())))
                .andExpect(jsonPath("$.data.points[1].value").value(57.8));
    }

    @Test
    void 기간을_주면_그_사이만_돌려준다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        log(token, animalId, "WEIGHT", LocalDate.of(2025, 5, 1), "\"value\": 40.0");
        log(token, animalId, "WEIGHT", LocalDate.of(2025, 6, 1), "\"value\": 42.0");

        mockMvc.perform(get("/api/v1/animals/" + animalId + "/weights").header(HttpHeaders.AUTHORIZATION, token)
                        .param("from", "2025-05-02").param("to", "2025-06-01"))
                .andExpect(jsonPath("$.data.points", hasSize(1)))
                .andExpect(jsonPath("$.data.points[0].date").value("2025-06-01"));
    }

    @Test
    void 남의_개체는_404() throws Exception {
        Integer animalId = createAnimal(tokenOfNewUser());

        mockMvc.perform(get("/api/v1/animals/" + animalId + "/weights").header(HttpHeaders.AUTHORIZATION, tokenOfNewUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    private void log(String token, Integer animalId, String type, LocalDate date, String fields) throws Exception {
        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": %d, \"type\": \"%s\", \"date\": \"%s\", %s }"
                                .formatted(animalId, type, date, fields)))
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
