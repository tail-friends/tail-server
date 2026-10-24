package com.tailfriends.animal.integration;

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

class AnimalDetailSummaryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 탈피_체중_캐릭터_기록이_개체_상세_요약에_나온다() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);
        LocalDate today = LocalDate.now(KstDate.ZONE);
        createLog(token, shed(animalId, today.minusDays(61)));
        createLog(token, shed(animalId, today.minusDays(31)));
        createLog(token, shed(animalId, today.minusDays(1)));
        createLog(token, weight(animalId, today.minusDays(10), "55.0"));
        createLog(token, weight(animalId, today.minusDays(2), "57.8"));
        mockMvc.perform(put("/api/v1/animals/" + animalId + "/character").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "shape": "gecko", "patternType": "spots",
                                  "colors": { "body": "#E0A33A", "pattern": "#3A2A1E", "belly": "#F2E6C8", "eye": "#2B2B2B" } }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shed.lastDate").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.data.shed.complete").value(true))
                .andExpect(jsonPath("$.data.shed.avgIntervalDays").value(30))
                .andExpect(jsonPath("$.data.shed.predictedDate").value(today.plusDays(29).toString()))
                .andExpect(jsonPath("$.data.shed.inBluePhase").value(false))
                .andExpect(jsonPath("$.data.weight.latest").value(57.8))
                .andExpect(jsonPath("$.data.weight.diff").value(2.8))
                .andExpect(jsonPath("$.data.character.shape").value("gecko"))
                .andExpect(jsonPath("$.data.character.colors.belly").value("#F2E6C8"))
                .andExpect(jsonPath("$.data.character.home").value("desert"))
                .andExpect(jsonPath("$.data.mood").value("JUST_SHED"));
    }

    @Test
    void 기록과_캐릭터가_없으면_요약은_비어_있고_기분은_NORMAL() throws Exception {
        String token = tokenOfNewUser();
        Integer animalId = createAnimal(token);

        mockMvc.perform(get("/api/v1/animals/" + animalId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shed.lastDate").isEmpty())
                .andExpect(jsonPath("$.data.shed.inBluePhase").value(false))
                .andExpect(jsonPath("$.data.weight.latest").isEmpty())
                .andExpect(jsonPath("$.data.character").isEmpty())
                .andExpect(jsonPath("$.data.mood").value("NORMAL"));
    }

    private static String shed(Integer animalId, LocalDate date) {
        return """
                { "clientId": "%s", "animalId": %d, "type": "SHED", "date": "%s", "stage": "SHED", "complete": true }
                """.formatted(UUID.randomUUID(), animalId, date);
    }

    private static String weight(Integer animalId, LocalDate date, String value) {
        return """
                { "clientId": "%s", "animalId": %d, "type": "WEIGHT", "date": "%s", "value": %s }
                """.formatted(UUID.randomUUID(), animalId, date, value);
    }

    private void createLog(String token, String body) throws Exception {
        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
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
