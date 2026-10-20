package com.tailfriends.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.dto.LogCreateRequest;
import com.tailfriends.log.dto.LogCreateResponse;
import com.tailfriends.log.dto.LogListResponse;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LogServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final Long USER_ID = 1L;
    private static final Long ANIMAL_ID = 12L;

    private final LogRepository logRepository = mock(LogRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LogService logService = new LogService(logRepository, animalRepository, userRepository, CLOCK);

    @Test
    void 기록일은_한국_날짜로_오늘까지_허용한다() {
        given(animalRepository.findByIdAndUserId(ANIMAL_ID, USER_ID)).willReturn(Optional.of(animal()));
        given(logRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 902L));

        Long id = logService.create(USER_ID, weightRequest(null, LocalDate.of(2026, 10, 20))).id();

        assertThat(id).isEqualTo(902L);
    }

    @Test
    void 기록일이_한국_날짜로_내일이면_INVALID_INPUT() {
        assertThatThrownBy(() -> logService.create(USER_ID, weightRequest(null, LocalDate.of(2026, 10, 21))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(logRepository, never()).save(any());
    }

    @Test
    void 남의_개체에는_기록할_수_없다() {
        given(animalRepository.findByIdAndUserId(ANIMAL_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> logService.create(USER_ID, weightRequest(null, LocalDate.of(2026, 10, 18))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    @Test
    void 이미_저장한_clientId면_저장하지_않고_기존_기록_ID를_담아_409() {
        UUID clientId = UUID.randomUUID();
        given(animalRepository.findByIdAndUserId(ANIMAL_ID, USER_ID)).willReturn(Optional.of(animal()));
        given(logRepository.findByUserIdAndClientId(USER_ID, clientId))
                .willReturn(Optional.of(withId(weightLog(LocalDate.of(2026, 10, 18)), 880L)));

        assertThatThrownBy(() -> logService.create(USER_ID, weightRequest(clientId, LocalDate.of(2026, 10, 18))))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CLIENT_ID);
                    assertThat(e.getData()).isEqualTo(new LogCreateResponse(880L));
                });
        verify(logRepository, never()).save(any());
    }

    @Test
    void 한_페이지보다_많으면_마지막_기록_ID를_nextCursor로_준다() {
        given(animalRepository.findByIdAndUserId(ANIMAL_ID, USER_ID)).willReturn(Optional.of(animal()));
        given(logRepository.findFirstPage(eq(ANIMAL_ID), any(), any())).willReturn(logsWithIds(30, 29, 28));

        LogListResponse response = logService.getLogs(USER_ID, ANIMAL_ID, null, null, 2);

        assertThat(response.logs()).extracting("id").containsExactly(30L, 29L);
        assertThat(response.nextCursor()).isEqualTo(29L);
    }

    @Test
    void 마지막_페이지면_nextCursor는_null() {
        given(animalRepository.findByIdAndUserId(ANIMAL_ID, USER_ID)).willReturn(Optional.of(animal()));
        given(logRepository.findFirstPage(eq(ANIMAL_ID), any(), any())).willReturn(logsWithIds(30, 29));

        LogListResponse response = logService.getLogs(USER_ID, ANIMAL_ID, null, null, 2);

        assertThat(response.logs()).hasSize(2);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 다른_개체의_기록을_cursor로_주면_INVALID_INPUT() {
        Animal other = animal();
        ReflectionTestUtils.setField(other, "id", 99L);
        Log otherLog = withId(Log.create(other, null, null, LogType.NOTE, LocalDate.of(2026, 10, 18), "메모", null), 50L);
        given(animalRepository.findByIdAndUserId(ANIMAL_ID, USER_ID)).willReturn(Optional.of(animal()));
        given(logRepository.findByIdAndUserId(50L, USER_ID)).willReturn(Optional.of(otherLog));

        assertThatThrownBy(() -> logService.getLogs(USER_ID, ANIMAL_ID, null, 50L, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(logRepository, never()).findPageAfter(anyLong(), any(), any(), anyLong(), any());
    }

    private static LogCreateRequest weightRequest(UUID clientId, LocalDate date) {
        return new LogCreateRequest(clientId, ANIMAL_ID, LogType.WEIGHT, date, null, null, null, null, null,
                null, null, new BigDecimal("52.3"), null, null, null, null, null);
    }

    private static Log weightLog(LocalDate date) {
        return Log.create(animal(), null, null, LogType.WEIGHT, date, null,
                new LogDetails(null, null, null, null, null, null, new BigDecimal("52.3"),
                        null, null, null, null, null));
    }

    private static List<Log> logsWithIds(long... ids) {
        return LongStream.of(ids).mapToObj(id -> withId(weightLog(LocalDate.of(2026, 10, 18)), id)).toList();
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static Animal animal() {
        return withId(Animal.builder()
                .user(User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build())
                .speciesName("레오파드 게코")
                .name("망고")
                .feedIntervalDays(4)
                .build(), ANIMAL_ID);
    }
}
