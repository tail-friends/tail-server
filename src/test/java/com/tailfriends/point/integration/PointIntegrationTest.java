package com.tailfriends.point.integration;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class PointIntegrationTest extends IntegrationTestSupport {

    private static final String FEED = "\"type\": \"FEED\", \"food\": \"귀뚜라미\"";
    private static final String CLEAN = "\"type\": \"CLEAN\", \"full\": true";
    private static final String WEIGHT = "\"type\": \"WEIGHT\", \"value\": 52.3";
    private static final String SHED = "\"type\": \"SHED\", \"stage\": \"SHED\"";
    private static final String ENV = "\"type\": \"ENV\", \"humidity\": 40";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 개체마다_종류마다_하루_한_번_10포인트를_적립하고_잔액과_내역에_남긴다() throws Exception {
        String token = tokenOfNewUser();
        long mango = createAnimal(token);
        long choco = createAnimal(token);

        writeLog(token, mango, FEED, "2026-10-20").andExpect(jsonPath("$.data.pointsEarned").value(10));
        writeLog(token, mango, FEED, "2026-10-21").andExpect(jsonPath("$.data.pointsEarned").value(0));
        // 지난 날짜로 적어도 오늘 이미 적립한 종류는 더 쌓이지 않는다
        writeLog(token, mango, FEED, "2026-09-01").andExpect(jsonPath("$.data.pointsEarned").value(0));
        writeLog(token, mango, CLEAN, "2026-10-20").andExpect(jsonPath("$.data.pointsEarned").value(10));
        writeLog(token, mango, ENV, "2026-10-20").andExpect(jsonPath("$.data.pointsEarned").value(0));
        writeLog(token, choco, FEED, "2026-10-20").andExpect(jsonPath("$.data.pointsEarned").value(10));

        mockMvc.perform(get("/api/v1/points/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(30))
                .andExpect(jsonPath("$.data.history", hasSize(3)))
                .andExpect(jsonPath("$.data.history[0].reason").value("LOG_FEED"))
                .andExpect(jsonPath("$.data.history[0].animalId").value(choco))
                .andExpect(jsonPath("$.data.history[1].reason").value("LOG_CLEAN"))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.points").value(30));
    }

    @Test
    void 기록을_지워도_포인트는_그대로고_다시_적어도_더_쌓이지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long mango = createAnimal(token);
        String body = writeLog(token, mango, WEIGHT, "2026-10-20")
                .andExpect(jsonPath("$.data.pointsEarned").value(10))
                .andReturn().getResponse().getContentAsString();
        long logId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        mockMvc.perform(delete("/api/v1/logs/" + logId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        writeLog(token, mango, WEIGHT, "2026-10-20").andExpect(jsonPath("$.data.pointsEarned").value(0));

        mockMvc.perform(get("/api/v1/points/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.balance").value(10))
                .andExpect(jsonPath("$.data.history", hasSize(1)))
                .andExpect(jsonPath("$.data.history[0].logId").isEmpty());
    }

    @Test
    void 일괄_기록도_개체마다_적립한다() throws Exception {
        String token = tokenOfNewUser();
        long mango = createAnimal(token);
        long choco = createAnimal(token);
        writeLog(token, mango, CLEAN, "2026-10-20");

        mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "CLEAN", "date": "2026-10-20",
                                  "items": [ { "animalId": %d, "full": true }, { "animalId": %d } ] }
                                """.formatted(mango, choco)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/points/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.balance").value(20));
    }

    @Test
    void 같은_종류를_동시에_적어도_한_번만_적립하고_다른_종류는_모두_쌓인다() throws Exception {
        String token = tokenOfNewUser();
        long mango = createAnimal(token);

        List<ResultActions> sameType = concurrently(List.of(FEED, FEED, FEED, FEED, FEED), token, mango);
        List<ResultActions> otherTypes = concurrently(List.of(CLEAN, WEIGHT, SHED), token, mango);

        assertThat(statuses(sameType)).containsOnly(201);
        assertThat(pointsEarned(sameType)).containsExactlyInAnyOrder(10, 0, 0, 0, 0);
        assertThat(statuses(otherTypes)).containsOnly(201);
        assertThat(pointsEarned(otherTypes)).containsOnly(10);
        mockMvc.perform(get("/api/v1/points/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.balance").value(40))
                .andExpect(jsonPath("$.data.history", hasSize(4)));
    }

    private List<ResultActions> concurrently(List<String> types, String token, long animalId) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(types.size());
        try {
            List<Future<ResultActions>> futures = new ArrayList<>();
            for (String type : types) {
                Callable<ResultActions> task = () -> {
                    start.await();
                    return writeLog(token, animalId, type, "2026-10-20");
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            List<ResultActions> results = new ArrayList<>();
            for (Future<ResultActions> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<Integer> statuses(List<ResultActions> results) {
        return results.stream().map(result -> result.andReturn().getResponse().getStatus()).toList();
    }

    private static List<Integer> pointsEarned(List<ResultActions> results) throws Exception {
        List<Integer> points = new ArrayList<>();
        for (ResultActions result : results) {
            points.add(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.pointsEarned"));
        }
        return points;
    }

    private ResultActions writeLog(String token, long animalId, String typeAndFields, String date) throws Exception {
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
