package com.tailfriends.point.integration;

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
import com.tailfriends.global.support.AdminTokens;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class ShopIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 관리자가_추가한_아이템을_포인트로_사면_보유_목록과_포인트_내역에_남는다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long driftwood = createItem(admin, "유목 S", "DRIFTWOOD", 50);
        User keeper = newUser();
        String token = tokenOf(keeper);

        shop(token).andExpect(jsonPath(itemPath(driftwood) + ".owned").value(false))
                .andExpect(jsonPath(itemPath(driftwood) + ".price").value(50));
        purchase(token, driftwood)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NOT_ENOUGH_POINTS"));

        givePoints(keeper, 120);
        purchase(token, driftwood)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(70));

        shop(token).andExpect(jsonPath(itemPath(driftwood) + ".owned").value(true));
        mockMvc.perform(get("/api/v1/points/my-items").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(driftwood))
                .andExpect(jsonPath("$.data.items[0].name").value("유목 S"))
                .andExpect(jsonPath("$.data.items[0].purchasedAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/points/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.balance").value(70))
                .andExpect(jsonPath("$.data.history", hasSize(1)))
                .andExpect(jsonPath("$.data.history[0].reason").value("PURCHASE"))
                .andExpect(jsonPath("$.data.history[0].amount").value(-50));

        // 다시 사면 잔액이 남아 있어도 막는다
        purchase(token, driftwood)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_OWNED"));
        assertThat(pointsOf(keeper)).isEqualTo(70);
    }

    @Test
    void 판매를_중지하면_상점과_구매에서_빠지지만_산_사람의_보유_목록에는_남는다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long plant = createItem(admin, "이끼", "PLANT", 30);
        User buyer = newUser();
        User late = newUser();
        givePoints(buyer, 30);
        givePoints(late, 30);
        purchase(tokenOf(buyer), plant).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/shop-items/" + plant).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"onSale\": false, \"price\": 10 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onSale").value(false))
                .andExpect(jsonPath("$.data.price").value(10));

        shop(tokenOf(late)).andExpect(jsonPath(itemPath(plant)).isEmpty());
        mockMvc.perform(get("/api/v1/points/items").param("category", "PLANT")
                        .header(HttpHeaders.AUTHORIZATION, tokenOf(late)))
                .andExpect(jsonPath(itemPath(plant)).isEmpty());
        purchase(tokenOf(late), plant)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/points/my-items").header(HttpHeaders.AUTHORIZATION, tokenOf(buyer)))
                .andExpect(jsonPath("$.data.items[0].id").value(plant));
        mockMvc.perform(get("/api/v1/admin/shop-items").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath(itemPath(plant) + ".onSale").value(false));
        // 가격을 바꿔도 산 사람의 포인트는 그대로다
        assertThat(pointsOf(buyer)).isZero();
        assertThat(pointsOf(late)).isEqualTo(30);
    }

    @Test
    void 같은_아이템을_동시에_사도_한_번만_사고_포인트도_한_번만_빠진다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long hide = createItem(admin, "은신처", "HIDE", 40);
        User keeper = newUser();
        givePoints(keeper, 200);

        List<Integer> statuses = purchaseConcurrently(tokenOf(keeper), List.of(hide, hide, hide, hide, hide));

        assertThat(statuses).containsOnlyOnce(200);
        assertThat(statuses).filteredOn(code -> code != 200).containsOnly(409);
        assertThat(pointsOf(keeper)).isEqualTo(160);
        assertThat(count("select count(*) from user_items where user_id = ?", keeper.getId())).isEqualTo(1);
        assertThat(count("select count(*) from point_histories where user_id = ?", keeper.getId())).isEqualTo(1);
    }

    @Test
    void 잔액이_하나만_살_만큼이면_다른_아이템을_동시에_사도_하나만_사고_잔액은_음수가_되지_않는다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        List<Long> items = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            items.add(createItem(admin, "배경 " + i, "BACKGROUND", 100));
        }
        User keeper = newUser();
        givePoints(keeper, 150);

        List<Integer> statuses = purchaseConcurrently(tokenOf(keeper), items);

        assertThat(statuses).containsOnlyOnce(200);
        assertThat(pointsOf(keeper)).isEqualTo(50);
        assertThat(count("select count(*) from user_items where user_id = ?", keeper.getId())).isEqualTo(1);
    }

    @Test
    void 아이템을_산_사용자도_탈퇴할_수_있다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long driftwood = createItem(admin, "유목 L", "DRIFTWOOD", 10);
        User keeper = newUser();
        givePoints(keeper, 10);
        purchase(tokenOf(keeper), driftwood).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, tokenOf(keeper)))
                .andExpect(status().isNoContent());

        assertThat(count("select count(*) from user_items where user_id = ?", keeper.getId())).isZero();
        assertThat(count("select count(*) from shop_items where id = ?", driftwood)).isEqualTo(1);
    }

    private List<Integer> purchaseConcurrently(String token, List<Long> itemIds) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(itemIds.size());
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (long itemId : itemIds) {
                Callable<Integer> task = () -> {
                    start.await();
                    return purchase(token, itemId).andReturn().getResponse().getStatus();
                };
                results.add(executor.submit(task));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    private ResultActions shop(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/points/items").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private static String itemPath(long itemId) {
        return "$.data.items[?(@.id == " + itemId + ")]";
    }

    private ResultActions purchase(String token, long itemId) throws Exception {
        return mockMvc.perform(post("/api/v1/points/items/" + itemId + "/purchase")
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    private long createItem(String admin, String name, String category, int price) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admin/shop-items").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "category": "%s", "price": %d,
                                  "imageUrl": "https://cdn.example.com/item.png" }
                                """.formatted(name, category, price)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private void givePoints(User user, int points) {
        jdbcTemplate.update("update users set points = points + ? where id = ?", points, user.getId());
    }

    private int pointsOf(User user) {
        return count("select points from users where id = ?", user.getId());
    }

    private int count(String sql, long id) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return value == null ? 0 : value;
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }

    private String tokenOf(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
