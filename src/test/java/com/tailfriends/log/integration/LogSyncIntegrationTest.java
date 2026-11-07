package com.tailfriends.log.integration;

import static org.hamcrest.Matchers.hasSize;
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
