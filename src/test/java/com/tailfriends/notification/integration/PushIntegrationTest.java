package com.tailfriends.notification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.FeedService;
import com.tailfriends.global.security.JwtProvider;
import com.tailfriends.global.support.IntegrationTestSupport;
import com.tailfriends.global.util.KstDate;
import com.tailfriends.notification.domain.FeedReminder;
import com.tailfriends.notification.fcm.PushSender;
import com.tailfriends.notification.repository.PushTokenRepository;
import com.tailfriends.notification.service.FeedReminderService;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FCM 호출만 가짜로 바꾸고 토큰 저장, 알림 대상 쿼리(알림 시간·동의), 급여 상태 판정은 실제 DB로 확인한다.
 */
class PushIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private FeedService feedService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private PushSender pushSender;

    @Test
    void 같은_토큰은_한_번만_저장하고_다른_계정이_등록하면_옮기며_로그아웃하면_지운다() throws Exception {
        User mango = saveUser();
        User choco = saveUser();
        String token = "fcm-" + UUID.randomUUID();

        register(token(mango), token, "IOS");
        register(token(mango), token, "IOS");
        assertThat(ownerOf(token)).isEqualTo(mango.getId());
        assertThat(countOf(token)).isEqualTo(1);

        register(token(choco), token, "ANDROID");
        assertThat(ownerOf(token)).isEqualTo(choco.getId());

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, token(mango))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"refreshToken\": \"none\", \"pushToken\": \"%s\" }".formatted(token)))
                .andExpect(status().isNoContent());
        assertThat(countOf(token)).as("남의 토큰은 지우지 않는다").isEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, token(choco))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"refreshToken\": \"none\", \"pushToken\": \"%s\" }".formatted(token)))
                .andExpect(status().isNoContent());
        assertThat(countOf(token)).isZero();
    }

    @Test
    void 알림_시간에_동의한_사용자에게_급여일_푸시를_보내고_쓸_수_없는_토큰은_지운다() throws Exception {
        // KST 21:30 구간
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 11, 7, 21, 34, 0, 0, KstDate.ZONE).toInstant(), ZoneOffset.UTC);
        LocalDate today = KstDate.today(Clock.systemUTC());

        User agreed = saveUser();
        String agreedToken = token(agreed);
        long mango = createAnimal(agreedToken, "망고", 4);
        long choco = createAnimal(agreedToken, "초코", 3);
        createAnimal(agreedToken, "보리", 5);
        feed(agreedToken, mango, today.minusDays(4));
        feed(agreedToken, choco, today.minusDays(5));
        String goodToken = "fcm-" + UUID.randomUUID();
        String deadToken = "fcm-" + UUID.randomUUID();
        register(agreedToken, goodToken, "IOS");
        register(agreedToken, deadToken, "ANDROID");
        setNotifyTime(agreed, "21:35");
        consent(agreed, false);
        consent(agreed, true);

        User refused = saveUser();
        String refusedToken = token(refused);
        feed(refusedToken, createAnimal(refusedToken, "콩이", 4), today.minusDays(4));
        register(refusedToken, "fcm-" + UUID.randomUUID(), "IOS");
        setNotifyTime(refused, "21:30");
        consent(refused, true);
        consent(refused, false);

        given(pushSender.send(anyList(), anyString(), anyString())).willReturn(List.of());
        given(pushSender.send(eq(List.of(goodToken, deadToken)), anyString(), anyString()))
                .willReturn(List.of(deadToken));

        new FeedReminderService(pushTokenRepository, animalRepository, feedService, pushSender,
                transactionTemplate, clock).sendDueReminders();

        verify(pushSender).send(List.of(goodToken, deadToken), FeedReminder.TITLE,
                "오늘 망고 밥 주는 날이에요. 초코 급여일이 2일 지났어요");
        verify(pushSender, never()).send(eq(tokensOf(refused)), anyString(), anyString());
        assertThat(countOf(deadToken)).isZero();
        assertThat(countOf(goodToken)).isEqualTo(1);
    }

    @Test
    void 알림_시간이_구간_밖이면_보내지_않는다() throws Exception {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 11, 7, 7, 0, 0, 0, KstDate.ZONE).toInstant(), ZoneOffset.UTC);
        User user = saveUser();
        String token = token(user);
        feed(token, createAnimal(token, "망고", 4), KstDate.today(Clock.systemUTC()).minusDays(4));
        String pushToken = "fcm-" + UUID.randomUUID();
        register(token, pushToken, "IOS");
        consent(user, true);
        // 설정이 없으면 09:00

        new FeedReminderService(pushTokenRepository, animalRepository, feedService, pushSender,
                transactionTemplate, clock).sendDueReminders();

        verify(pushSender, never()).send(eq(List.of(pushToken)), anyString(), anyString());
    }

    @Test
    void 자정_직전_구간은_23시_59분_알림도_보낸다() throws Exception {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 11, 7, 23, 50, 0, 0, KstDate.ZONE).toInstant(), ZoneOffset.UTC);
        User user = saveUser();
        String token = token(user);
        feed(token, createAnimal(token, "망고", 4), KstDate.today(Clock.systemUTC()).minusDays(4));
        String pushToken = "fcm-" + UUID.randomUUID();
        register(token, pushToken, "IOS");
        setNotifyTime(user, "23:59");
        consent(user, true);
        given(pushSender.send(anyList(), anyString(), anyString())).willReturn(List.of());

        new FeedReminderService(pushTokenRepository, animalRepository, feedService, pushSender,
                transactionTemplate, clock).sendDueReminders();

        verify(pushSender).send(List.of(pushToken), FeedReminder.TITLE, "오늘 망고 밥 주는 날이에요");
    }

    private void register(String bearer, String pushToken, String platform) throws Exception {
        mockMvc.perform(post("/api/v1/users/me/push-tokens").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"%s\", \"platform\": \"%s\" }".formatted(pushToken, platform)))
                .andExpect(status().isNoContent());
    }

    private void feed(String bearer, long animalId, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/v1/logs").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": %d, \"type\": \"FEED\", \"date\": \"%s\", \"food\": \"귀뚜라미\" }"
                                .formatted(animalId, date)))
                .andExpect(status().isCreated());
    }

    private long createAnimal(String bearer, String name, int feedIntervalDays) throws Exception {
        String response = mockMvc.perform(post("/api/v1/animals").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"%s\", \"speciesId\": 1, \"feedIntervalDays\": %d }"
                                .formatted(name, feedIntervalDays)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    private void setNotifyTime(User user, String time) {
        jdbcTemplate.update("""
                INSERT INTO user_settings (user_id, notify_time, created_at, updated_at)
                VALUES (?, CAST(? AS TIME), NOW(), NOW())
                """, user.getId(), time);
    }

    private void consent(User user, boolean agreed) {
        jdbcTemplate.update("""
                INSERT INTO consents (user_id, type, agreed, created_at, updated_at)
                VALUES (?, 'NOTIFICATION', ?, NOW(), NOW())
                """, user.getId(), agreed);
    }

    private List<String> tokensOf(User user) {
        return jdbcTemplate.queryForList("SELECT token FROM push_tokens WHERE user_id = ?", String.class,
                user.getId());
    }

    private Long ownerOf(String pushToken) {
        return jdbcTemplate.queryForObject("SELECT user_id FROM push_tokens WHERE token = ?", Long.class, pushToken);
    }

    private Integer countOf(String pushToken) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM push_tokens WHERE token = ?", Integer.class,
                pushToken);
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .provider(Provider.KAKAO)
                .providerId(UUID.randomUUID().toString())
                .nickname("망고집사")
                .build());
    }

    private String token(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getRole());
    }
}
