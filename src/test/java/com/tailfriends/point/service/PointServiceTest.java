package com.tailfriends.point.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.point.domain.PointHistory;
import com.tailfriends.point.domain.PointReason;
import com.tailfriends.point.dto.PointHistoryListResponse;
import com.tailfriends.point.repository.PointHistoryRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class PointServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate KST_TODAY = LocalDate.of(2026, 10, 20);
    private static final Long USER_ID = 1L;

    private final PointHistoryRepository pointHistoryRepository = mock(PointHistoryRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PointService pointService =
            new PointService(CurrentUserStub.of(USER_ID), pointHistoryRepository, userRepository, CLOCK);

    @Test
    void 처음_적은_급여_기록이면_10포인트를_적립하고_잔액에_더한다() {
        given(pointHistoryRepository.insertEarning(USER_ID, 12L, 902L, 10, "LOG_FEED", KST_TODAY)).willReturn(1);

        int earned = pointService.earnForLog(log(LogType.FEED, LocalDate.of(2026, 10, 20)));

        assertThat(earned).isEqualTo(10);
        verify(userRepository).addPoints(USER_ID, 10);
    }

    @Test
    void 오늘_같은_종류를_이미_적립했으면_0이고_잔액을_바꾸지_않는다() {
        given(pointHistoryRepository.insertEarning(anyLong(), anyLong(), anyLong(), anyInt(), anyString(), any()))
                .willReturn(0);

        assertThat(pointService.earnForLog(log(LogType.CLEAN, KST_TODAY))).isZero();
        verify(userRepository, never()).addPoints(anyLong(), anyInt());
    }

    @Test
    void 지난_날짜의_기록도_기록한_날_한국_날짜로_하루_한_번만_센다() {
        given(pointHistoryRepository.insertEarning(anyLong(), anyLong(), anyLong(), anyInt(), anyString(), any()))
                .willReturn(1);

        pointService.earnForLog(log(LogType.WEIGHT, LocalDate.of(2026, 9, 1)));

        verify(pointHistoryRepository).insertEarning(USER_ID, 12L, 902L, 10, "LOG_WEIGHT", KST_TODAY);
    }

    @Test
    void 온습도_메모_기록은_적립하지_않는다() {
        assertThat(pointService.earnForLog(log(LogType.NOTE, KST_TODAY))).isZero();
        verify(pointHistoryRepository, never())
                .insertEarning(anyLong(), anyLong(), anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void 내역은_잔액과_함께_돌려주고_더_있으면_마지막_ID를_커서로_준다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(userWithPoints(30)));
        given(pointHistoryRepository.findPage(eq(USER_ID), eq(Long.MAX_VALUE), eq(PageRequest.of(0, 3))))
                .willReturn(List.of(history(503L), history(502L), history(501L)));

        PointHistoryListResponse response = pointService.getHistory(null, 2);

        assertThat(response.balance()).isEqualTo(30);
        assertThat(response.history()).extracting("id").containsExactly(503L, 502L);
        assertThat(response.nextCursor()).isEqualTo(502L);
    }

    @Test
    void 마지막_페이지면_커서가_없다() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(userWithPoints(10)));
        given(pointHistoryRepository.findPage(eq(USER_ID), eq(502L), any())).willReturn(List.of(history(501L)));

        PointHistoryListResponse response = pointService.getHistory(502L, 2);

        assertThat(response.history()).extracting("id").containsExactly(501L);
        assertThat(response.nextCursor()).isNull();
    }

    private static Log log(LogType type, LocalDate date) {
        User user = withId(User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build(),
                USER_ID);
        Animal animal = withId(Animal.builder().user(user).speciesName("레오파드 게코").name("망고")
                .feedIntervalDays(4).build(), 12L);
        LogDetails details = type == LogType.WEIGHT
                ? new LogDetails(null, null, null, null, null, null, new BigDecimal("50.0"),
                null, null, null, null, null)
                : new LogDetails(null, null, null, null, null, null, null, null, null, null, null, null);
        String memo = type == LogType.NOTE ? "활발함" : null;
        return withId(Log.create(animal, user, null, type, date, memo, details), 902L);
    }

    private static User userWithPoints(int points) {
        User user = User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build();
        ReflectionTestUtils.setField(user, "points", points);
        return user;
    }

    private static PointHistory history(Long id) {
        try {
            Constructor<PointHistory> constructor = PointHistory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PointHistory history = withId(constructor.newInstance(), id);
            ReflectionTestUtils.setField(history, "amount", 10);
            ReflectionTestUtils.setField(history, "reason", PointReason.LOG_FEED);
            return history;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
