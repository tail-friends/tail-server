package com.tailfriends.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.domain.VetVisit;
import com.tailfriends.health.dto.VetVisitCreateRequest;
import com.tailfriends.health.dto.VetVisitCreateRequest.MedicationSchedule;
import com.tailfriends.health.dto.VetVisitCreateResponse;
import com.tailfriends.health.dto.VetVisitResponse;
import com.tailfriends.health.dto.VetVisitUpdateRequest;
import com.tailfriends.health.repository.VetVisitRepository;
import com.tailfriends.schedule.domain.RepeatType;
import com.tailfriends.schedule.domain.ScheduleKind;
import com.tailfriends.schedule.domain.Weekday;
import com.tailfriends.schedule.dto.RepeatRequest;
import com.tailfriends.schedule.dto.ScheduleCreateRequest;
import com.tailfriends.schedule.dto.ScheduleCreateResponse;
import com.tailfriends.schedule.service.ScheduleService;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class VetVisitServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate KST_TODAY = LocalDate.of(2026, 10, 20);
    private static final Long USER_ID = 1L;

    private final VetVisitRepository vetVisitRepository = mock(VetVisitRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final ScheduleService scheduleService = mock(ScheduleService.class);
    private final VetVisitService service = new VetVisitService(CurrentUserStub.of(USER_ID), vetVisitRepository,
            animalRepository, scheduleService, CLOCK);

    @Test
    void 진료_기록을_저장하고_투약_일정이_없으면_scheduleId는_null() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal()));
        given(vetVisitRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 8L));

        VetVisitCreateResponse response = service.create(request(KST_TODAY, LocalDate.of(2026, 11, 3), null));

        assertThat(response).isEqualTo(new VetVisitCreateResponse(8L, null));
        verify(scheduleService, never()).create(any());
    }

    @Test
    void 투약_일정을_함께_보내면_MEDICATION_종류로_오늘부터_만든다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal()));
        given(vetVisitRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 8L));
        given(scheduleService.create(any())).willReturn(new ScheduleCreateResponse(41L, KST_TODAY));
        RepeatRequest twiceAWeek = new RepeatRequest(RepeatType.WEEKDAYS, null, Set.of(Weekday.MON, Weekday.THU));

        // 열흘 전 진료를 오늘 적어도 투약은 오늘부터
        VetVisitCreateResponse response = service.create(request(KST_TODAY.minusDays(10), null,
                new MedicationSchedule("온욕", twiceAWeek, LocalDate.of(2026, 10, 31))));

        assertThat(response.scheduleId()).isEqualTo(41L);
        ArgumentCaptor<ScheduleCreateRequest> captor = ArgumentCaptor.forClass(ScheduleCreateRequest.class);
        verify(scheduleService).create(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new ScheduleCreateRequest(12L, "온욕", ScheduleKind.MEDICATION,
                twiceAWeek, KST_TODAY, LocalDate.of(2026, 10, 31)));
    }

    @Test
    void 방문일은_한국_날짜로_오늘까지_된다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal()));
        given(vetVisitRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 8L));

        service.create(request(KST_TODAY, null, null));

        assertInvalidInput(() -> service.create(request(KST_TODAY.plusDays(1), null, null)));
    }

    @Test
    void 다음_방문일이_방문일보다_빠르거나_같으면_저장하지_않는다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.of(animal()));

        assertInvalidInput(() -> service.create(request(KST_TODAY, KST_TODAY, null)));
        assertInvalidInput(() -> service.create(request(KST_TODAY, KST_TODAY.minusDays(1), null)));
        verify(vetVisitRepository, never()).save(any());
    }

    @Test
    void 남의_개체에는_진료_기록을_적을_수_없다() {
        given(animalRepository.findByIdAndUserId(12L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(KST_TODAY, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    @Test
    void 수정은_보낸_필드만_바꾸고_바꾼_뒤의_날짜_규칙을_확인한다() {
        VetVisit visit = visit(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 16));
        given(vetVisitRepository.findByIdAndUserId(8L, USER_ID)).willReturn(Optional.of(visit));

        VetVisitResponse response = service.update(8L,
                new VetVisitUpdateRequest(null, null, null, "탈피 부전 (호전)", null, null));
        assertThat(response.diagnosis()).isEqualTo("탈피 부전 (호전)");
        assertThat(response.hospital()).isEqualTo("제주 특수동물병원");

        // 방문일만 다음 방문일 뒤로 옮기면 규칙을 어긴다
        assertInvalidInput(() -> service.update(8L,
                new VetVisitUpdateRequest(null, LocalDate.of(2026, 10, 17), null, null, null, null)));
        assertThat(visit.getVisitDate()).isEqualTo(LocalDate.of(2026, 10, 2));
    }

    @Test
    void 남의_진료_기록은_VISIT_NOT_FOUND() {
        given(vetVisitRepository.findByIdAndUserId(8L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(8L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VISIT_NOT_FOUND);
        verify(vetVisitRepository, never()).delete(any());
    }

    private static VetVisitCreateRequest request(LocalDate visitDate, LocalDate nextVisitDate,
                                                 MedicationSchedule medication) {
        return new VetVisitCreateRequest(12L, " 제주 특수동물병원 ", visitDate, "부분 탈피", "탈피 부전", "온욕 주 2회",
                nextVisitDate, medication);
    }

    private static VetVisit visit(LocalDate visitDate, LocalDate nextVisitDate) {
        return withId(VetVisit.builder().animal(animal()).hospital("제주 특수동물병원").visitDate(visitDate)
                .diagnosis("탈피 부전").nextVisitDate(nextVisitDate).build(), 8L);
    }

    private static Animal animal() {
        User user = withId(User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build(),
                USER_ID);
        return withId(Animal.builder().user(user).speciesName("레오파드 게코").name("망고").feedIntervalDays(4).build(),
                12L);
    }

    private static void assertInvalidInput(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
