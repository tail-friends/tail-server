package com.tailfriends.log.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

class LogBatchIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 여러_개체를_한_번에_기록하고_5초_안에_되돌린다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        Integer choco = createAnimal(token);

        String body = mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "FEED", "date": "2026-09-01",
                                  "items": [ { "animalId": %d, "food": "귀뚜라미", "count": 6 },
                                             { "animalId": %d, "food": "해동 마우스", "count": 1, "refused": true } ] }
                                """.formatted(mango, choco)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.logIds", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        String batchId = JsonPath.read(body, "$.data.batchId");
        Instant undoUntil = Instant.parse(JsonPath.read(body, "$.data.undoUntil"));
        assertThat(undoUntil).isAfter(Instant.now());

        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", choco.toString()))
                .andExpect(jsonPath("$.data.logs[0].food").value("해동 마우스"))
                .andExpect(jsonPath("$.data.logs[0].refused").value(true));

        mockMvc.perform(delete("/api/v1/logs/batch/" + batchId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(countOfBatch(batchId)).isZero();
    }

    @Test
    void 하나라도_실패하면_아무것도_저장하지_않는다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        Integer choco = createAnimal(token);

        mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "WEIGHT", "date": "2026-09-01",
                                  "items": [ { "animalId": %d, "value": 52.3 }, { "animalId": %d } ] }
                                """.formatted(mango, choco)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));

        mockMvc.perform(get("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                        .param("animalId", mango.toString()))
                .andExpect(jsonPath("$.data.logs", hasSize(0)));
    }

    @Test
    void 저장하고_5초가_지나면_되돌릴_수_없다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        String body = mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"CLEAN\", \"date\": \"2026-09-01\", \"items\": [ { \"animalId\": %d, \"full\": true } ] }"
                                .formatted(mango)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String batchId = JsonPath.read(body, "$.data.batchId");
        // 저장 시각을 10초 전으로 옮겨 5초가 지난 상황을 만든다
        jdbcTemplate.update("update logs set created_at = created_at - interval '10 seconds' where batch_id = ?::uuid",
                batchId);

        mockMvc.perform(delete("/api/v1/logs/batch/" + batchId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("UNDO_EXPIRED"));
        assertThat(countOfBatch(batchId)).isEqualTo(1);
    }

    @Test
    void 남의_개체가_섞이면_404이고_남의_묶음은_되돌릴_수_없다() throws Exception {
        String owner = tokenOfNewUser();
        String other = tokenOfNewUser();
        Integer mine = createAnimal(owner);
        Integer theirs = createAnimal(other);

        mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"CLEAN\", \"date\": \"2026-09-01\", \"items\": [ { \"animalId\": %d }, { \"animalId\": %d } ] }"
                                .formatted(mine, theirs)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));

        String body = mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"CLEAN\", \"date\": \"2026-09-01\", \"items\": [ { \"animalId\": %d } ] }"
                                .formatted(mine)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String batchId = JsonPath.read(body, "$.data.batchId");

        mockMvc.perform(delete("/api/v1/logs/batch/" + batchId).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BATCH_NOT_FOUND"));
    }

    @Test
    void 이미_저장한_clientId를_다시_보내면_409와_기존_기록_ID를_돌려준다() throws Exception {
        String token = tokenOfNewUser();
        Integer mango = createAnimal(token);
        String clientId = UUID.randomUUID().toString();
        String request = """
                { "type": "CLEAN", "date": "2026-09-01", "items": [ { "clientId": "%s", "animalId": %d } ] }
                """.formatted(clientId, mango);
        String body = mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer logId = JsonPath.read(body, "$.data.logIds[0]");

        mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CLIENT_ID"))
                .andExpect(jsonPath("$.data.id").value(logId));
    }

    private Integer createAnimal(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 4 }"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private Integer countOfBatch(String batchId) {
        return jdbcTemplate.queryForObject("select count(*) from logs where batch_id = ?::uuid", Integer.class, batchId);
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
