package com.tailfriends.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.schedule.domain.Repeat;
import com.tailfriends.schedule.domain.RepeatType;
import com.tailfriends.schedule.domain.Schedule;
import com.tailfriends.schedule.domain.ScheduleKind;
import com.tailfriends.schedule.dto.RepeatRequest;
import com.tailfriends.schedule.dto.ScheduleCompleteRequest;
import com.tailfriends.schedule.dto.ScheduleCreateRequest;
import com.tailfriends.schedule.dto.ScheduleCreateResponse;
import com.tailfriends.schedule.repository.ScheduleRepository;
import com.tailfriends.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScheduleServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final Long USER_ID = 1L;

    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ScheduleService service = new ScheduleService(
            CurrentUserStub.of(USER_ID), scheduleRepository, animalRepository, userRepository, CLOCK);

    @Test
    void 남의_개체로_일정을_만들면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(12L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void 개체_없이_만들면_공통_일정이다() {
        given(scheduleRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ScheduleCreateResponse response = service.create(request(null));

        assertThat(response.nextDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        verify(animalRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void 한국_날짜로_오늘까지는_완료할_수_있고_내일은_INVALID_INPUT() {
        Schedule schedule = Schedule.create(null, null, "더스팅", ScheduleKind.SUPPLEMENT, Repeat.everyNDays(3),
                LocalDate.of(2026, 10, 18), null);
        given(scheduleRepository.findByIdAndUserId(41L, USER_ID)).willReturn(Optional.of(schedule));

        assertThat(service.complete(41L, new ScheduleCompleteRequest(LocalDate.of(2026, 10, 20))).nextDate())
                .isEqualTo(LocalDate.of(2026, 10, 23));
        assertThatThrownBy(() -> service.complete(41L, new ScheduleCompleteRequest(LocalDate.of(2026, 10, 21))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 없거나_남의_일정이면_SCHEDULE_NOT_FOUND() {
        given(scheduleRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    void 남의_개체로_목록을_조회하면_ANIMAL_NOT_FOUND() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSchedules(12L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    private static ScheduleCreateRequest request(Long animalId) {
        return new ScheduleCreateRequest(animalId, " UVB 램프 교체 ", ScheduleKind.EQUIPMENT,
                new RepeatRequest(RepeatType.EVERY_N_DAYS, 180, null), LocalDate.of(2026, 10, 18), null);
    }
}
