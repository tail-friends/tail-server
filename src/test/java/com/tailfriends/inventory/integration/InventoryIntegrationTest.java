package com.tailfriends.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
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

class InventoryIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate TODAY = LocalDate.now(KstDate.ZONE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void 먹은_급여_기록만_같은_이름의_마리_품목에서_빼고_0_아래로는_내려가지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long animalId = createAnimal(token);
        long cricket = createItem(token, "귀뚜라미", 100, "COUNT", 30);
        long mealworm = createItem(token, "밀웜", 500, "GRAM", null);

        feed(token, animalId, "귀뚜라미", 6, false).andExpect(status().isCreated());
        // 안 먹음은 빼지 않는다
        feed(token, animalId, "귀뚜라미", 6, true).andExpect(status().isCreated());
        // 앞뒤 공백은 빼고 비교한다
        feed(token, animalId, " 귀뚜라미 ", 4, false).andExpect(status().isCreated());
        // g 품목은 빼지 않는다
        feed(token, animalId, "밀웜", 10, false).andExpect(status().isCreated());
        // 이름이 다르면 빼지 않는다
        feed(token, animalId, "귀뚜라미 성충", 3, false).andExpect(status().isCreated());

        assertThat(quantityOf(token, cricket)).isEqualTo(90);
        assertThat(quantityOf(token, mealworm)).isEqualTo(500);

        feed(token, animalId, "귀뚜라미", 200, false).andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.items[?(@.id == " + cricket + ")].quantity").value(0))
                .andExpect(jsonPath("$.data.items[?(@.id == " + cricket + ")].low").value(true));
    }

    @Test
    void 일괄_급여_기록도_빼고_되돌려도_다시_더하지_않는다() throws Exception {
        String token = tokenOfNewUser();
        long mango = createAnimal(token);
        long choco = createAnimal(token);
        long cricket = createItem(token, "귀뚜라미", 50, "COUNT", null);

        String body = mockMvc.perform(post("/api/v1/logs/batch").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "type": "FEED", "date": "%s",
                                  "items": [ { "animalId": %d, "food": "귀뚜라미", "count": 6 },
                                             { "animalId": %d, "food": "귀뚜라미", "count": 4, "refused": true } ] }
                                """.formatted(TODAY, mango, choco)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(quantityOf(token, cricket)).isEqualTo(44);

        String batchId = JsonPath.read(body, "$.data.batchId");
        mockMvc.perform(delete("/api/v1/logs/batch/" + batchId).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        assertThat(quantityOf(token, cricket)).isEqualTo(44);
    }

    @Test
    void 같은_품목을_동시에_빼도_수량이_맞고_남의_재고는_건드리지_않는다() throws Exception {
        String token = tokenOfNewUser();
        String other = tokenOfNewUser();
        long animalId = createAnimal(token);
        long cricket = createItem(token, "귀뚜라미", 100, "COUNT", null);
        long othersCricket = createItem(other, "귀뚜라미", 100, "COUNT", null);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return feed(token, animalId, "귀뚜라미", 2, false).andReturn().getResponse().getStatus();
                };
                results.add(executor.submit(task));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(201);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(quantityOf(token, cricket)).isEqualTo(90);
        assertThat(quantityOf(other, othersCricket)).isEqualTo(100);
    }

    @Test
    void 등록_중복_채우기_삭제와_남의_품목() throws Exception {
        String token = tokenOfNewUser();
        String other = tokenOfNewUser();
        long cricket = createItem(token, "귀뚜라미", 20, "COUNT", 30);

        mockMvc.perform(post("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \" 귀뚜라미 \", \"quantity\": 1, \"unit\": \"COUNT\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ITEM"));
        // 다른 사용자는 같은 이름을 쓸 수 있다
        createItem(other, "귀뚜라미", 1, "COUNT", null);

        update(token, cricket, "{ \"add\": 100 }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(120))
                .andExpect(jsonPath("$.data.low").value(false));
        update(token, cricket, "{ \"quantity\": 5 }")
                .andExpect(jsonPath("$.data.quantity").value(5))
                .andExpect(jsonPath("$.data.low").value(true));
        update(token, cricket, "{ \"add\": 1, \"quantity\": 1 }").andExpect(status().isBadRequest());
        update(other, cricket, "{ \"add\": 1 }")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVENTORY_ITEM_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/inventory/" + cricket).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    private ResultActions feed(String token, long animalId, String food, int count, boolean refused) throws Exception {
        return mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "animalId": %d, "type": "FEED", "date": "%s", "food": "%s", "count": %d, "refused": %s }
                        """.formatted(animalId, TODAY, food, count, refused)));
    }

    private ResultActions update(String token, long itemId, String body) throws Exception {
        return mockMvc.perform(patch("/api/v1/inventory/" + itemId).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private int quantityOf(String token, long itemId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token))
                .andReturn().getResponse().getContentAsString();
        List<Integer> quantities = JsonPath.read(body, "$.data.items[?(@.id == " + itemId + ")].quantity");
        return quantities.getFirst();
    }

    private long createItem(String token, String name, int quantity, String unit, Integer alertBelow)
            throws Exception {
        String body = mockMvc.perform(post("/api/v1/inventory").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"%s\", \"quantity\": %d, \"unit\": \"%s\", \"alertBelow\": %s }"
                                .formatted(name, quantity, unit, alertBelow)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
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
