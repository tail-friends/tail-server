package com.tailfriends.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.service.HealthAlertService;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.dto.LogBatchRequest;
import com.tailfriends.log.dto.LogBatchRequest.Item;
import com.tailfriends.log.dto.LogBatchResponse;
import com.tailfriends.log.dto.LogIdResponse;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.point.service.PointService;
import com.tailfriends.schedule.service.ScheduleService;
import com.tailfriends.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LogBatchServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Instant NOW = Instant.parse("2026-10-19T15:30:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final Long USER_ID = 1L;
    private static final UUID CLIENT_A = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    private static final UUID CLIENT_B = UUID.fromString("00000000-0000-4000-8000-00000000000b");

    private final LogRepository logRepository = mock(LogRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final HealthAlertService healthAlertService = mock(HealthAlertService.class);
    private final ScheduleService scheduleService = mock(ScheduleService.class);
    private final PointService pointService = mock(PointService.class);
    private final LogBatchService service = new LogBatchService(CurrentUserStub.of(USER_ID), logRepository,
            animalRepository, userRepository, healthAlertService, scheduleService, pointService, CLOCK);

    private final Animal mango = animal(12L);
    private final Animal choco = animal(15L);

    @BeforeEach
    void setUp() {
        given(animalRepository.findAllByUserIdOrderByIdAsc(USER_ID)).willReturn(List.of(mango, choco));
        AtomicLong ids = new AtomicLong(900);
        given(logRepository.save(any())).willAnswer(invocation -> {
            Log log = invocation.getArgument(0);
            ReflectionTestUtils.setField(log, "id", ids.incrementAndGet());
            ReflectionTestUtils.setField(log, "createdAt", NOW);
            return log;
        });
    }

    @Test
    void 같은_묶음으로_저장하고_요청_순서대로_ID와_되돌리기_기한을_돌려준다() {
        LogBatchResponse response = service.create(feedBatch(item(CLIENT_A, 15L), item(CLIENT_B, 12L)));

        assertThat(response.logIds()).containsExactly(901L, 902L);
        assertThat(response.undoUntil()).isEqualTo(NOW.plusSeconds(5));
        verify(healthAlertService).evaluate(choco, LogType.FEED);
        verify(healthAlertService).evaluate(mango, LogType.FEED);
    }

    @Test
    void 더스팅한_항목의_개체만_더스팅_일정을_완료한다() {
        Item dusted = new Item(null, 12L, null, "귀뚜라미", 6, false, true, null, null, null);
        Item notDusted = new Item(null, 15L, null, "귀뚜라미", 6, false, false, null, null, null);

        service.create(feedBatch(dusted, notDusted));

        verify(scheduleService).completeDusting(USER_ID, 12L, TODAY);
        verify(scheduleService, never()).completeDusting(USER_ID, 15L, TODAY);
    }

    @Test
    void 급여_청소_체중이_아니면_INVALID_INPUT() {
        assertError(() -> service.create(new LogBatchRequest(LogType.SHED, TODAY, List.of(item(null, 12L)))),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 오늘_이후_날짜면_INVALID_INPUT() {
        assertError(() -> service.create(new LogBatchRequest(LogType.FEED, TODAY.plusDays(1),
                List.of(item(null, 12L)))), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 같은_개체나_같은_clientId가_두_번이면_INVALID_INPUT() {
        assertError(() -> service.create(feedBatch(item(null, 12L), item(null, 12L))), ErrorCode.INVALID_INPUT);
        assertError(() -> service.create(feedBatch(item(CLIENT_A, 12L), item(CLIENT_A, 15L))), ErrorCode.INVALID_INPUT);
    }

    @Test
    void 내_개체가_아닌_것이_하나라도_있으면_아무것도_저장하지_않고_ANIMAL_NOT_FOUND() {
        assertError(() -> service.create(feedBatch(item(null, 12L), item(null, 99L))), ErrorCode.ANIMAL_NOT_FOUND);
        verify(logRepository, never()).save(any());
    }

    @Test
    void 이미_저장된_clientId가_있으면_items_순서로_첫_번째_기존_기록_ID와_함께_DUPLICATE_CLIENT_ID() {
        Log savedB = Log.create(mango, null, CLIENT_B, LogType.FEED, TODAY, null, null);
        Log savedA = Log.create(choco, null, CLIENT_A, LogType.FEED, TODAY, null, null);
        ReflectionTestUtils.setField(savedB, "id", 870L);
        ReflectionTestUtils.setField(savedA, "id", 880L);
        given(logRepository.findAllByUserIdAndClientIdIn(any(), anyList())).willReturn(List.of(savedB, savedA));

        assertThatThrownBy(() -> service.create(feedBatch(item(CLIENT_A, 15L), item(CLIENT_B, 12L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException exception = (BusinessException) e;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CLIENT_ID);
                    assertThat(exception.getData()).isEqualTo(new LogIdResponse(880L));
                });
        verify(logRepository, never()).save(any());
    }

    @Test
    void 체중_묶음에_값이_없는_항목이_있으면_INVALID_INPUT() {
        Item withValue = new Item(null, 12L, null, null, null, null, null, new BigDecimal("52.3"), null, null);
        Item withoutValue = new Item(null, 15L, null, null, null, null, null, null, null, null);

        assertError(() -> service.create(new LogBatchRequest(LogType.WEIGHT, TODAY, List.of(withValue, withoutValue))),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 저장_후_5초까지는_되돌리고_지나면_UNDO_EXPIRED() {
        UUID batchId = UUID.randomUUID();
        Log log = Log.create(mango, null, null, LogType.FEED, TODAY, null, null);
        given(logRepository.findAllByUserIdAndBatchIdOrderByIdAsc(USER_ID, batchId)).willReturn(List.of(log));

        ReflectionTestUtils.setField(log, "createdAt", NOW.minusSeconds(5));
        service.undo(batchId);
        verify(logRepository).deleteAll(List.of(log));

        ReflectionTestUtils.setField(log, "createdAt", NOW.minusSeconds(6));
        assertError(() -> service.undo(batchId), ErrorCode.UNDO_EXPIRED);
    }

    @Test
    void 없거나_남의_일괄_기록이면_BATCH_NOT_FOUND() {
        UUID batchId = UUID.randomUUID();
        given(logRepository.findAllByUserIdAndBatchIdOrderByIdAsc(USER_ID, batchId)).willReturn(List.of());

        assertError(() -> service.undo(batchId), ErrorCode.BATCH_NOT_FOUND);
    }

    private static LogBatchRequest feedBatch(Item... items) {
        return new LogBatchRequest(LogType.FEED, TODAY, List.of(items));
    }

    private static Item item(UUID clientId, Long animalId) {
        return new Item(clientId, animalId, null, "귀뚜라미", 6, false, false, null, null, null);
    }

    private static Animal animal(Long id) {
        Animal animal = Animal.builder().speciesName("레오파드 게코").name("개체" + id).feedIntervalDays(4).build();
        ReflectionTestUtils.setField(animal, "id", id);
        return animal;
    }

    private static void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
