package com.tailfriends.enclosure.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

class EnclosureIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 처음_목록을_보면_기본_테마_사육장_5개가_테마_순서로_생기고_동시에_봐도_늘지_않는다() throws Exception {
        User keeper = newUser();
        String token = tokenOf(keeper);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Callable<Integer> task = () -> {
                    start.await();
                    return list(token).andReturn().getResponse().getStatus();
                };
                results.add(executor.submit(task));
            }
            start.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(200);
            }
        } finally {
            executor.shutdownNow();
        }

        list(token)
                .andExpect(jsonPath("$.data.enclosures", hasSize(5)))
                .andExpect(jsonPath("$.data.enclosures[*].background.theme")
                        .value(contains("desert", "forest", "rock", "night", "pond")))
                .andExpect(jsonPath("$.data.enclosures[0].background.type").value("BUILT_IN"))
                .andExpect(jsonPath("$.data.enclosures[0].decorationCount").value(0));
        assertThat(count("select count(*) from enclosures where user_id = ?", keeper.getId())).isEqualTo(5);
    }

    @Test
    void 산_아이템을_놓으면_상세와_목록에_나오고_다시_저장하면_통째로_바뀐다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long driftwood = createItem(admin, "유목 S");
        long plant = createItem(admin, "이끼");
        User keeper = newUser();
        String token = tokenOf(keeper);
        buy(keeper, driftwood);
        buy(keeper, plant);
        long desert = enclosureId(token, 0);

        save(token, desert, """
                { "decorations": [ { "itemId": %d, "x": 0.33333, "y": 0.85, "flip": true },
                                   { "itemId": %d, "x": 0.7, "y": 0.9 },
                                   { "itemId": %d, "x": 0.1, "y": 0.2 } ] }
                """.formatted(plant, driftwood, plant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decorations", hasSize(3)))
                .andExpect(jsonPath("$.data.decorations[0].x").value(0.333))
                .andExpect(jsonPath("$.data.decorations[1].flip").value(false));

        mockMvc.perform(get("/api/v1/enclosures/" + desert).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.background.theme").value("desert"))
                .andExpect(jsonPath("$.data.decorations[*].itemId").value(contains((int) plant, (int) driftwood,
                        (int) plant)))
                .andExpect(jsonPath("$.data.decorations[0].x").value(0.333))
                .andExpect(jsonPath("$.data.decorations[0].y").value(0.85))
                .andExpect(jsonPath("$.data.animalIds").isEmpty());
        list(token).andExpect(jsonPath("$.data.enclosures[0].decorationCount").value(3))
                .andExpect(jsonPath("$.data.enclosures[1].decorationCount").value(0));

        save(token, desert, "{ \"decorations\": [ { \"itemId\": %d, \"x\": 0.5, \"y\": 0.5 } ] }".formatted(driftwood))
                .andExpect(status().isOk());
        list(token).andExpect(jsonPath("$.data.enclosures[0].decorationCount").value(1));

        save(token, desert, "{ \"decorations\": [] }").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decorations").isEmpty());
        assertThat(count("select count(*) from enclosure_decorations where enclosure_id = ?", desert)).isZero();
    }

    @Test
    void 사지_않은_아이템은_놓을_수_없고_남의_사육장은_404() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long driftwood = createItem(admin, "유목 M");
        long hide = createItem(admin, "은신처 M");
        User keeper = newUser();
        String token = tokenOf(keeper);
        String other = tokenOf(newUser());
        buy(keeper, driftwood);
        long desert = enclosureId(token, 0);
        save(token, desert, "{ \"decorations\": [ { \"itemId\": %d, \"x\": 0.5, \"y\": 0.5 } ] }".formatted(driftwood))
                .andExpect(status().isOk());

        save(token, desert, """
                { "decorations": [ { "itemId": %d, "x": 0.5, "y": 0.5 }, { "itemId": %d, "x": 0.1, "y": 0.1 } ] }
                """.formatted(driftwood, hide))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_OWNED"));
        // 실패하면 원래 배치가 남는다
        assertThat(count("select count(*) from enclosure_decorations where enclosure_id = ?", desert)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/enclosures/" + desert).header(HttpHeaders.AUTHORIZATION, other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENCLOSURE_NOT_FOUND"));
        save(other, desert, "{ \"decorations\": [] }")
                .andExpect(status().isNotFound());
        assertThat(count("select count(*) from enclosure_decorations where enclosure_id = ?", desert)).isEqualTo(1);
    }

    @Test
    void 판매를_중지한_아이템도_산_것이면_놓을_수_있다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long background = createItem(admin, "노을 배경");
        User keeper = newUser();
        String token = tokenOf(keeper);
        buy(keeper, background);
        mockMvc.perform(patch("/api/v1/admin/shop-items/" + background).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"onSale\": false }"))
                .andExpect(status().isOk());

        save(token, enclosureId(token, 3),
                "{ \"decorations\": [ { \"itemId\": %d, \"x\": 0, \"y\": 1 } ] }".formatted(background))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decorations[0].itemId").value(background));
    }

    @Test
    void 장식을_놓은_사용자도_탈퇴할_수_있다() throws Exception {
        String admin = AdminTokens.newAdmin(userRepository, jwtProvider);
        long driftwood = createItem(admin, "유목 L");
        User keeper = newUser();
        String token = tokenOf(keeper);
        buy(keeper, driftwood);
        long desert = enclosureId(token, 0);
        save(token, desert, "{ \"decorations\": [ { \"itemId\": %d, \"x\": 0.5, \"y\": 0.5 } ] }".formatted(driftwood))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(count("select count(*) from enclosures where user_id = ?", keeper.getId())).isZero();
        assertThat(count("select count(*) from enclosure_decorations where enclosure_id = ?", desert)).isZero();
        assertThat(count("select count(*) from shop_items where id = ?", driftwood)).isEqualTo(1);
    }

    private ResultActions list(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/enclosures").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    private long enclosureId(String token, int index) throws Exception {
        String body = list(token).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.enclosures[" + index + "].id")).longValue();
    }

    private ResultActions save(String token, long enclosureId, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/enclosures/" + enclosureId + "/decorations")
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long createItem(String admin, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admin/shop-items").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "category": "DRIFTWOOD", "price": 10,
                                  "imageUrl": "https://cdn.example.com/item.png" }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private void buy(User user, long itemId) throws Exception {
        jdbcTemplate.update("update users set points = points + 10 where id = ?", user.getId());
        mockMvc.perform(post("/api/v1/points/items/" + itemId + "/purchase")
                        .header(HttpHeaders.AUTHORIZATION, tokenOf(user)))
                .andExpect(status().isOk());
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
