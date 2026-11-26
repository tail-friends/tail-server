package com.tailfriends.log.integration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class LogSyncIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 실패한_항목은_건너뛰고_나머지를_저장하며_다시_보내면_DUPLICATE() throws Exception {
        String token = tokenOfNewUser();
        String other = tokenOfNewUser();
        Integer mango = createAnimal(token);
        Integer theirs = createAnimal(other);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        String request = """
                { "logs": [
                  { "clientId": "%s", "animalId": %d, "type": "WEIGHT", "date": "2026-09-01", "value": 58.1 },
                  { "clientId": "%s", "animalId": %d, "type": "WEIGHT", "date": "2026-09-01", "value": 40.0 },
                  { "clientId": "%s", "animalId": %d, "type": "NOTE", "date": "2026-09-02", "memo": "밥 잘 먹음" }
                ] }
                """.formatted(a, mango, b, theirs, c, mango);

        String body = mockMvc.perform(post("/api/v1/logs/sync").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.results[1].status").value("FAILED"))
                .andExpect(jsonPath("$.data.results[1].error.code").value("ANIMAL_NOT_FOUND"))
                .andExpect(jsonPath("$.data.results[2].status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();
        Integer firstId = JsonPath.read(body, "$.data.results[0].id");

        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", mango.toString()))
                .andExpect(jsonPath("$.data.logs", hasSize(2)));

        mockMvc.perform(post("/api/v1/logs/sync").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.results[0].id").value(firstId))
                .andExpect(jsonPath("$.data.results[2].status").value("DUPLICATE"));
        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", mango.toString()))
                .andExpect(jsonPath("$.data.logs", hasSize(2)));
    }

    @Test
    void 한_요청에_같은_clientId가_두_번이면_두_번째는_DUPLICATE() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        UUID a = UUID.randomUUID();
        String item = "{ \"clientId\": \"%s\", \"animalId\": %d, \"type\": \"CLEAN\", \"date\": \"2026-09-01\" }"
                .formatted(a, mango);

        mockMvc.perform(post("/api/v1/logs/sync").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"logs\": [" + item + "," + item + "] }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.results[1].status").value("DUPLICATE"));
    }

    @Test
    void 동기화한_기록도_포인트_재고_건강_경고를_한_건씩_기록할_때와_같이_처리한다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        mockMvc.perform(post("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"귀뚜라미\", \"quantity\": 50, \"unit\": \"COUNT\" }"))
                .andExpect(status().isCreated());
        String fed = "{ \"clientId\": \"%s\", \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", "
                + "\"food\": \"귀뚜라미\", \"count\": 4 }";
        String refused = "{ \"clientId\": \"%s\", \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", "
                + "\"food\": \"귀뚜라미\", \"count\": 1, \"refused\": true }";
        String logs = String.join(",",
                fed.formatted(UUID.randomUUID(), mango, "2026-08-30"),
                refused.formatted(UUID.randomUUID(), mango, "2026-08-31"),
                refused.formatted(UUID.randomUUID(), mango, "2026-09-01"),
                refused.formatted(UUID.randomUUID(), mango, "2026-09-02"));

        mockMvc.perform(post("/api/v1/logs/sync").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"logs\": [" + logs + "] }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[*].status").value(everyItem(is("CREATED"))));

        mockMvc.perform(get("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.items[0].quantity").value(46));
        mockMvc.perform(get("/api/v1/points/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.balance").value(10));
        mockMvc.perform(get("/api/v1/health/alerts").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", mango.toString()))
                .andExpect(jsonPath("$.data.alerts", hasSize(1)))
                .andExpect(jsonPath("$.data.alerts[0].type").value("REFUSAL_ALERT"));
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
