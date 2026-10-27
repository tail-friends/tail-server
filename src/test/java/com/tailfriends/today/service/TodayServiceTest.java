package com.tailfriends.today.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.FeedSchedule;
import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.FeedService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.repository.HealthAlertRepository;
import com.tailfriends.health.repository.OpenAlert;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.repository.DoneLog;
import com.tailfriends.log.repository.FedFood;
import com.tailfriends.log.repository.LogRepository;
import com.tailfriends.photo.repository.AnimalThumbnail;
import com.tailfriends.photo.repository.PhotoRepository;
import com.tailfriends.photo.storage.PhotoStorage;
import com.tailfriends.schedule.domain.Repeat;
import com.tailfriends.schedule.domain.Schedule;
import com.tailfriends.schedule.domain.ScheduleKind;
import com.tailfriends.schedule.repository.ScheduleRepository;
import com.tailfriends.today.dto.TodayResponse;
import com.tailfriends.today.dto.TodayResponse.TodayDone;
import com.tailfriends.today.dto.TodayResponse.TodayFeed;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TodayServiceTest {

    // UTC 2026-10-19 15:30 = KST 2026-10-20 00:30
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-19T15:30:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final Long USER_ID = 1L;

    private final AnimalRepository animalRepository = mock(AnimalRepository.class);
    private final FeedService feedService = mock(FeedService.class);
    private final LogRepository logRepository = mock(LogRepository.class);
    private final PhotoRepository photoRepository = mock(PhotoRepository.class);
    private final PhotoStorage photoStorage = mock(PhotoStorage.class);
    private final ScheduleRepository scheduleRepository = mock(ScheduleRepository.class);
    private final HealthAlertRepository healthAlertRepository = mock(HealthAlertRepository.class);
    private final TodayService service = new TodayService(CurrentUserStub.of(USER_ID), animalRepository, feedService,
            logRepository, photoRepository, photoStorage, scheduleRepository, healthAlertRepository, CLOCK);

    private final Animal mango = animal(12L, "망고");
    private final Animal choco = animal(15L, "초코");
    private final Animal kiwi = animal(18L, "키위");
    private final Animal nori = animal(21L, "노리");

    @BeforeEach
    void setUp() {
        given(animalRepository.findAllByUserIdOrderByIdAsc(USER_ID)).willReturn(List.of(mango, choco, kiwi, nori));
        given(feedService.schedulesOf(anyList())).willReturn(Map.of(
                12L, new FeedSchedule(FeedStatus.DUE, TODAY.minusDays(4), TODAY, 0),
                15L, new FeedSchedule(FeedStatus.OVERDUE, TODAY.minusDays(9), TODAY.minusDays(2), -2),
                18L, new FeedSchedule(FeedStatus.UPCOMING, TODAY, TODAY.plusDays(4), 4),
                21L, new FeedSchedule(FeedStatus.NO_RECORD, null, null, null)));
    }

    @Test
    void 급여가_오늘이거나_지난_개체만_다음_급여일이_이른_순으로_준다() {
        TodayResponse response = service.getToday(null);

        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.feeds()).extracting(TodayFeed::animalId).containsExactly(15L, 12L);
        assertThat(response.feeds()).extracting(TodayFeed::overdueDays).containsExactly(2, null);
    }

    @Test
    void 최근_캐릭터_사진과_최근에_먹은_먹이를_채운다() {
        given(photoRepository.findCharacterThumbnails(anyList())).willReturn(List.of(
                new AnimalThumbnail(12L, "thumb/new.jpg"), new AnimalThumbnail(12L, "thumb/old.jpg")));
        given(photoStorage.urlOf("thumb/new.jpg")).willReturn("https://files/thumb/new.jpg");
        given(logRepository.findLatestFedFoods(eq(12L), any())).willReturn(List.of(new FedFood("귀뚜라미", 6)));

        List<TodayFeed> feeds = service.getToday(null).feeds();

        TodayFeed mangoFeed = feeds.get(1);
        assertThat(mangoFeed.thumbnailUrl()).isEqualTo("https://files/thumb/new.jpg");
        assertThat(mangoFeed.suggested().food()).isEqualTo("귀뚜라미");
        assertThat(mangoFeed.suggested().count()).isEqualTo(6);
        assertThat(feeds.get(0).thumbnailUrl()).isNull();
        assertThat(feeds.get(0).suggested()).isNull();
    }

    @Test
    void 오늘_남긴_기록_다음에_오늘_완료한_일정을_준다() {
        Schedule lamp = Schedule.create(null, null, "UVB 램프 교체", ScheduleKind.EQUIPMENT, Repeat.everyNDays(180),
                TODAY, null);
        ReflectionTestUtils.setField(lamp, "id", 42L);
        given(logRepository.findDoneLogs(USER_ID, TODAY)).willReturn(List.of(new DoneLog(899L, 12L, LogType.CLEAN)));
        given(scheduleRepository.findDoneOfUser(USER_ID, TODAY)).willReturn(List.of(lamp));

        List<TodayDone> done = service.getToday(null).done();

        assertThat(done).containsExactly(
                new TodayDone("CLEAN", 12L, 899L, null),
                new TodayDone(TodayDone.SCHEDULE, null, null, 42L));
    }

    @Test
    void 열린_경고를_그대로_준다() {
        given(healthAlertRepository.findOpenByUser(USER_ID)).willReturn(List.of(
                new OpenAlert(77L, 15L, AlertType.REFUSAL_ALERT)));

        assertThat(service.getToday(TODAY).alerts()).singleElement()
                .satisfies(alert -> assertThat(alert.type()).isEqualTo(AlertType.REFUSAL_ALERT));
    }

    @Test
    void 오늘이_아닌_날짜면_INVALID_INPUT() {
        assertThatThrownBy(() -> service.getToday(TODAY.minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private static Animal animal(Long id, String name) {
        Animal animal = Animal.builder().speciesName("레오파드 게코").name(name).feedIntervalDays(4).build();
        ReflectionTestUtils.setField(animal, "id", id);
        return animal;
    }
}
