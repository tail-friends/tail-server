package com.tailfriends.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.auth.domain.RefreshToken;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.service.TokenService;
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

/**
 * 쓴 리프레시 토큰이 다시 오면 그 로그인(토큰 묶음)을 끊는지 실제 DB로 확인한다.
 */
class RefreshTokenReuseIntegrationTest extends IntegrationTestSupport {

    private static final int CONCURRENT_REISSUES = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 쓴_지_10초가_지난_토큰이_다시_오면_그_로그인의_새_토큰까지_못_쓴다() throws Exception {
        TokenResponse login = tokenService.issue(saveUser());
        String stolen = login.refreshToken();
        String renewed = reissuedRefreshToken(stolen);
        usedSecondsAgo(stolen, 20);

        reissue(stolen)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        reissue(renewed)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
        assertThat(countOfFamily(stolen)).isNull();
        assertThat(countByHash(renewed)).isZero();
    }

    @Test
    void 한_로그인을_끊어도_다른_기기의_로그인은_그대로다() throws Exception {
        User user = saveUser();
        String phone = tokenService.issue(user).refreshToken();
        String tablet = tokenService.issue(user).refreshToken();
        reissuedRefreshToken(phone);
        usedSecondsAgo(phone, 20);

        reissue(phone).andExpect(status().isUnauthorized());

        reissue(tablet).andExpect(status().isOk());
    }

    @Test
    void 쓴_지_10초_안에_다시_오면_401만_주고_먼저_받은_새_토큰은_쓸_수_있다() throws Exception {
        String first = tokenService.issue(saveUser()).refreshToken();
        String renewed = reissuedRefreshToken(first);

        reissue(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        reissue(renewed).andExpect(status().isOk());
    }

    @Test
    void 같은_토큰으로_동시에_재발급하면_하나만_성공하고_로그인은_유지된다() throws Exception {
        String refreshToken = tokenService.issue(saveUser()).refreshToken();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REISSUES);
        List<String> bodies = new ArrayList<>();
        List<Integer> statuses = new ArrayList<>();
        try {
            List<Future<ResultActions>> results = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_REISSUES; i++) {
                Callable<ResultActions> task = () -> {
                    start.await();
                    return reissue(refreshToken);
                };
                results.add(executor.submit(task));
            }
            start.countDown();

            for (Future<ResultActions> result : results) {
                var response = result.get().andReturn().getResponse();
                statuses.add(response.getStatus());
                bodies.add(response.getContentAsString());
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 401).hasSize(CONCURRENT_REISSUES - 1);

        String renewed = JsonPath.read(bodies.get(statuses.indexOf(200)), "$.data.refreshToken");
        reissue(renewed).andExpect(status().isOk());
    }

    @Test
    void 로그아웃하면_그_로그인에서_쓴_토큰까지_묶음째_지운다() throws Exception {
        TokenResponse login = tokenService.issue(saveUser());
        String renewed = reissuedRefreshToken(login.refreshToken());
        assertThat(countOfFamily(renewed)).isEqualTo(2);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + renewed + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(countByHash(login.refreshToken())).isZero();
        assertThat(countByHash(renewed)).isZero();
    }

    private String reissuedRefreshToken(String refreshToken) throws Exception {
        String body = reissue(refreshToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.refreshToken");
    }

    private ResultActions reissue(String refreshToken) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    // 쓴 시각을 앞으로 옮겨 유예 시간이 지난 상황을 만든다
    private void usedSecondsAgo(String refreshToken, int seconds) {
        jdbcTemplate.update("update refresh_tokens set used_at = now() - make_interval(secs => ?) where token_hash = ?",
                seconds, RefreshToken.hash(refreshToken));
    }

    /**
     * 그 토큰이 속한 묶음의 행 수. 토큰이 없으면 null.
     */
    private Integer countOfFamily(String refreshToken) {
        List<Integer> counts = jdbcTemplate.queryForList("""
                select count(*) from refresh_tokens where family_id =
                    (select family_id from refresh_tokens where token_hash = ?)
                having count(*) > 0
                """, Integer.class, RefreshToken.hash(refreshToken));
        return counts.isEmpty() ? null : counts.get(0);
    }

    private Integer countByHash(String refreshToken) {
        return jdbcTemplate.queryForObject("select count(*) from refresh_tokens where token_hash = ?",
                Integer.class, RefreshToken.hash(refreshToken));
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("사용자")
                .build());
    }
}
