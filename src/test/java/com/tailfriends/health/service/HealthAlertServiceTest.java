package com.tailfriends.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.domain.HealthAlert;
import com.tailfriends.health.dto.AlertResponse;
import com.tailfriends.health.dto.AlertUpdateRequest;
import com.tailfriends.health.repository.HealthAlertRepository;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.Stool;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.species.domain.Home;
import com.tailfriends.species.domain.Shape;
import com.tailfriends.species.domain.Species;
import com.tailfriends.user.domain.AlertRules;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.service.UserSettingsService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class HealthAlertServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final Long USER_ID = 1L;
    private static final Long ANIMAL_ID = 12L;

    private final HealthAlertRepository alertRepository = mock(HealthAlertRepository.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final UserSettingsService userSettingsService = mock(UserSettingsService.class);
    private final HealthAlertService service =
            new HealthAlertService(CurrentUserStub.of(USER_ID), alertRepository, logRepository, animalRepository, userSettingsService, CLOCK);

    @BeforeEach
    void setUp() {
        given(userSettingsService.alertRulesOf(USER_ID)).willReturn(AlertRules.DEFAULT);
    }

    @Test
    void 안_먹음이_3번_연속이면_거식_경고를_만든다() {
        given(logRepository.findLatestRefused(eq(ANIMAL_ID), any())).willReturn(List.of(true, true, true));
        given(alertRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 77L));

        List<AlertResponse> alerts = service.evaluate(animal(), LogType.FEED);

        assertThat(alerts).extracting(AlertResponse::type).containsExactly(AlertType.REFUSAL_ALERT);
        assertThat(alerts.getFirst().message()).isEqualTo("최근 3번 연속으로 먹지 않았어요");
    }

    @Test
    void 개체_주인이_바꾼_경고_기준으로_판정한다() {
        given(userSettingsService.alertRulesOf(USER_ID)).willReturn(new AlertRules(2, 10, 30, 2));
        given(logRepository.findLatestRefused(eq(ANIMAL_ID), eq(PageRequest.of(0, 2)))).willReturn(List.of(true, true));
        given(alertRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 77L));

        List<AlertResponse> alerts = service.evaluate(animal(), LogType.FEED);

        assertThat(alerts).extracting(AlertResponse::message).containsExactly("최근 2번 연속으로 먹지 않았어요");
    }

    @Test
    void 같은_종류의_열린_경고가_있으면_새로_만들지_않는다() {
        given(logRepository.findLatestRefused(eq(ANIMAL_ID), any())).willReturn(List.of(true, true, true));
        given(alertRepository.existsByAnimalIdAndTypeAndStatus(ANIMAL_ID, AlertType.REFUSAL_ALERT, AlertStatus.OPEN))
                .willReturn(true);

        assertThat(service.evaluate(animal(), LogType.FEED)).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    void 쿨링_기간에는_거식_경고를_만들지_않는다() {
        Animal cooling = animal();
        ReflectionTestUtils.setField(cooling, "coolingStart", LocalDate.of(2026, 10, 1));
        given(logRepository.findLatestRefused(eq(ANIMAL_ID), any())).willReturn(List.of(true, true, true));

        assertThat(service.evaluate(cooling, LogType.FEED)).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    void 최근_체중을_그_기록일로부터_30일_안의_최고_체중과_비교한다() {
        LocalDate latestDate = LocalDate.of(2026, 10, 18);
        given(logRepository.findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(ANIMAL_ID, LogType.WEIGHT))
                .willReturn(Optional.of(weightLog(latestDate, "50.0")));
        given(logRepository.findHighestWeight(ANIMAL_ID, latestDate.minusDays(30), latestDate))
                .willReturn(Optional.of(new BigDecimal("57.0")));
        given(alertRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 78L));

        List<AlertResponse> alerts = service.evaluate(animal(), LogType.WEIGHT);

        assertThat(alerts).extracting(AlertResponse::message).containsExactly("최근 30일 동안 체중이 12% 줄었어요");
    }

    @Test
    void 묽은_변이_2번_연속이면_배변_경고를_만든다() {
        given(logRepository.findLatestStools(eq(ANIMAL_ID), any())).willReturn(List.of(Stool.LOOSE, Stool.LOOSE));
        given(alertRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 79L));

        assertThat(service.evaluate(animal(), LogType.CLEAN))
                .extracting(AlertResponse::type).containsExactly(AlertType.STOOL_ALERT);
    }

    @Test
    void 판정_규칙이_없는_종류의_기록은_경고를_만들지_않는다() {
        assertThat(service.evaluate(animal(), LogType.NOTE)).isEmpty();
        assertThat(service.evaluate(animal(), LogType.SHED)).isEmpty();
    }

    @Test
    void 확인_처리는_ACKNOWLEDGED만_된다() {
        HealthAlert alert = withId(HealthAlert.open(animal(), AlertType.STOOL_ALERT, "메시지"), 79L);
        given(alertRepository.findByIdAndUserId(79L, 1L)).willReturn(Optional.of(alert));

        assertThatThrownBy(() -> service.update(79L, new AlertUpdateRequest(AlertStatus.OPEN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(service.update(79L, new AlertUpdateRequest(AlertStatus.ACKNOWLEDGED)).status())
                .isEqualTo(AlertStatus.ACKNOWLEDGED);
    }

    @Test
    void 없거나_남의_경고는_ALERT_NOT_FOUND() {
        given(alertRepository.findByIdAndUserId(79L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(79L, new AlertUpdateRequest(AlertStatus.ACKNOWLEDGED)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALERT_NOT_FOUND);
    }

    @Test
    void 최근_온습도_기록이_종의_범위를_벗어나면_환경_경고를_만든다() {
        Animal gecko = animalOf(species());
        given(logRepository.findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(ANIMAL_ID, LogType.ENV))
                .willReturn(Optional.of(envLog(gecko, "29.0", "24.0", 35)));
        given(alertRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 80L));

        List<AlertResponse> alerts = service.evaluate(gecko, LogType.ENV);

        assertThat(alerts).extracting(AlertResponse::type).containsExactly(AlertType.ENV_WARNING);
        assertThat(alerts.getFirst().message()).isEqualTo("핫존 온도가 적정 범위(31~35℃)보다 낮아요");
    }

    @Test
    void 온습도가_범위_안이면_환경_경고를_만들지_않는다() {
        Animal gecko = animalOf(species());
        given(logRepository.findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(ANIMAL_ID, LogType.ENV))
                .willReturn(Optional.of(envLog(gecko, "32.0", "24.0", 35)));

        assertThat(service.evaluate(gecko, LogType.ENV)).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    @Test
    void 목록에_없는_종은_환경_경고를_판정하지_않는다() {
        assertThat(service.evaluate(animal(), LogType.ENV)).isEmpty();
        verify(logRepository, never()).findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(any(), eq(LogType.ENV));
    }

    @Test
    void 환경_경고가_열려_있으면_새로_만들지_않는다() {
        Animal gecko = animalOf(species());
        given(logRepository.findFirstByAnimalIdAndTypeOrderByLogDateDescIdDesc(ANIMAL_ID, LogType.ENV))
                .willReturn(Optional.of(envLog(gecko, "40.0", "24.0", 35)));
        given(alertRepository.existsByAnimalIdAndTypeAndStatus(ANIMAL_ID, AlertType.ENV_WARNING, AlertStatus.OPEN))
                .willReturn(true);

        assertThat(service.evaluate(gecko, LogType.ENV)).isEmpty();
        verify(alertRepository, never()).save(any());
    }

    private static Log envLog(Animal animal, String hot, String cool, Integer humidity) {
        return Log.create(animal, null, null, LogType.ENV, LocalDate.of(2026, 10, 19), null,
                new LogDetails(null, null, null, null, null, null, null,
                        null, null, new BigDecimal(hot), new BigDecimal(cool), humidity));
    }

    // 범위는 테스트용 값이다
    private static Species species() {
        return Species.builder()
                .name("레오파드 게코")
                .shape(Shape.GECKO)
                .feedIntervalDays(4)
                .hotMin(new BigDecimal("31.0"))
                .hotMax(new BigDecimal("35.0"))
                .coolMin(new BigDecimal("22.0"))
                .coolMax(new BigDecimal("26.0"))
                .humidityMin(30)
                .humidityMax(40)
                .defaultHome(Home.DESERT)
                .build();
    }

    private static Animal animalOf(Species species) {
        Animal animal = animal();
        ReflectionTestUtils.setField(animal, "species", species);
        return animal;
    }

    private static Log weightLog(LocalDate date, String weight) {
        return Log.create(animal(), null, null, LogType.WEIGHT, date, null,
                new LogDetails(null, null, null, null, null, null, new BigDecimal(weight),
                        null, null, null, null, null));
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static Animal animal() {
        return withId(Animal.builder()
                .user(withId(User.builder().provider(Provider.KAKAO).providerId("kakao-1").nickname("사용자").build(), USER_ID))
                .speciesName("레오파드 게코")
                .name("망고")
                .feedIntervalDays(4)
                .build(), ANIMAL_ID);
    }
}
