package com.tailfriends.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.dto.LogCreateRequest;
import com.tailfriends.log.dto.LogCreateResponse;
import com.tailfriends.log.dto.LogIdResponse;
import com.tailfriends.log.dto.LogSyncRequest;
import com.tailfriends.log.dto.LogSyncResult;
import com.tailfriends.log.dto.LogSyncResult.Status;
import com.tailfriends.log.repository.LogRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class LogSyncServiceTest {

    private static final Long USER_ID = 1L;
    private static final UUID CLIENT_A = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    private static final UUID CLIENT_B = UUID.fromString("00000000-0000-4000-8000-00000000000b");
    private static final UUID CLIENT_C = UUID.fromString("00000000-0000-4000-8000-00000000000c");

    private final LogService logService = mock(LogService.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final LogSyncService service =
            new LogSyncService(CurrentUserStub.of(USER_ID), logService, logRepository, validator);

    @Test
    void 항목마다_따로_저장하고_실패한_항목이_있어도_나머지는_저장한다() {
        LogCreateRequest a = weight(CLIENT_A, 12L);
        LogCreateRequest b = weight(CLIENT_B, 99L);
        LogCreateRequest c = weight(CLIENT_C, 12L);
        given(logService.create(a)).willReturn(created(901L));
        given(logService.create(b)).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        given(logService.create(c)).willReturn(created(902L));

        List<LogSyncResult> results = service.sync(new LogSyncRequest(List.of(a, b, c))).results();

        assertThat(results).extracting(LogSyncResult::status)
                .containsExactly(Status.CREATED, Status.FAILED, Status.CREATED);
        assertThat(results.get(0).id()).isEqualTo(901L);
        assertThat(results.get(1).error().code()).isEqualTo("ANIMAL_NOT_FOUND");
        assertThat(results.get(2).id()).isEqualTo(902L);
    }

    @Test
    void 이미_저장된_clientId는_DUPLICATE와_기존_기록_ID를_돌려준다() {
        LogCreateRequest a = weight(CLIENT_A, 12L);
        given(logService.create(a)).willThrow(new BusinessException(ErrorCode.DUPLICATE_CLIENT_ID,
                ErrorCode.DUPLICATE_CLIENT_ID.getMessage(), new LogIdResponse(700L)));

        LogSyncResult result = service.sync(new LogSyncRequest(List.of(a))).results().getFirst();

        assertThat(result.status()).isEqualTo(Status.DUPLICATE);
        assertThat(result.id()).isEqualTo(700L);
        assertThat(result.error()).isNull();
    }

    @Test
    void 동시에_저장돼_유니크_제약에_걸리면_기존_기록을_찾아_DUPLICATE로_돌려준다() {
        LogCreateRequest a = weight(CLIENT_A, 12L);
        Log existing = mock(Log.class);
        given(existing.getId()).willReturn(701L);
        given(logService.create(a)).willThrow(new DataIntegrityViolationException("uk_logs_user_id_client_id"));
        given(logRepository.findByUserIdAndClientId(USER_ID, CLIENT_A)).willReturn(Optional.of(existing));

        LogSyncResult result = service.sync(new LogSyncRequest(List.of(a))).results().getFirst();

        assertThat(result.status()).isEqualTo(Status.DUPLICATE);
        assertThat(result.id()).isEqualTo(701L);
    }

    @Test
    void clientId가_없거나_검증에_실패한_항목은_저장하지_않고_FAILED() {
        LogCreateRequest noClientId = weight(null, 12L);
        LogCreateRequest negative = new LogCreateRequest(CLIENT_A, 12L, LogType.WEIGHT, LocalDate.of(2026, 10, 17),
                null, null, null, null, null, null, null, new BigDecimal("-1"), null, null, null, null, null);

        List<LogSyncResult> results = service.sync(new LogSyncRequest(List.of(noClientId, negative))).results();

        assertThat(results).extracting(LogSyncResult::status).containsExactly(Status.FAILED, Status.FAILED);
        assertThat(results).extracting(r -> r.error().code()).containsOnly("INVALID_INPUT");
        assertThat(results.get(1).clientId()).isEqualTo(CLIENT_A);
        verify(logService, never()).create(any());
    }

    private static LogCreateRequest weight(UUID clientId, Long animalId) {
        return new LogCreateRequest(clientId, animalId, LogType.WEIGHT, LocalDate.of(2026, 10, 17),
                null, null, null, null, null, null, null, new BigDecimal("58.1"), null, null, null, null, null);
    }

    private static LogCreateResponse created(Long id) {
        return new LogCreateResponse(id, 0, List.of(), null);
    }
}
