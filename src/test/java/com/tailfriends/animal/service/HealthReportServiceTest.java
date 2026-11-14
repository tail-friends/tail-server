package com.tailfriends.animal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.animal.domain.Animal;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.animal.service.HealthReport.WeightEntry;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.CurrentUserStub;
import com.tailfriends.health.domain.VetVisit;
import com.tailfriends.health.repository.VetVisitRepository;
import com.tailfriends.log.domain.Log;
import com.tailfriends.log.domain.LogDetails;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.repository.LogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HealthReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 8, 11);
    private static final LocalDate TO = LocalDate.of(2026, 11, 11);
    private static final Animal MANGO = Animal.builder()
            .name("망고").speciesName("레오파드 게코").morph("탠저린").sex(Sex.FEMALE)
            .hatchDate(LocalDate.of(2025, 6, 20)).feedIntervalDays(4).build();

    @Test
    void 개체_정보와_기간_안_체중을_날짜순으로_담는다() {
        HealthReport report = HealthReportService.summarize(MANGO, FROM, TO, List.of(
                weight(LocalDate.of(2026, 9, 1), "48.0"),
                feed(LocalDate.of(2026, 9, 2), false),
                weight(LocalDate.of(2026, 10, 3), "57.8")), List.of());

        assertThat(report.generatedOn()).isEqualTo(TO);
        assertThat(report.profile().name()).isEqualTo("망고");
        assertThat(report.profile().species()).isEqualTo("레오파드 게코");
        assertThat(report.profile().feedIntervalDays()).isEqualTo(4);
        assertThat(report.weights()).containsExactly(
                new WeightEntry(LocalDate.of(2026, 9, 1), new BigDecimal("48.0")),
                new WeightEntry(LocalDate.of(2026, 10, 3), new BigDecimal("57.8")));
    }

    @Test
    void 급여_요약은_안_먹음_수와_마지막으로_먹은_날과_끝에서부터_연속_안_먹음을_센다() {
        HealthReport report = HealthReportService.summarize(MANGO, FROM, TO, List.of(
                feed(LocalDate.of(2026, 9, 1), true),
                feed(LocalDate.of(2026, 9, 5), false),
                feed(LocalDate.of(2026, 9, 9), true),
                feed(LocalDate.of(2026, 9, 13), true)), List.of());

        assertThat(report.feed().total()).isEqualTo(4);
        assertThat(report.feed().refused()).isEqualTo(3);
        assertThat(report.feed().eaten()).isEqualTo(1);
        assertThat(report.feed().lastEatenDate()).isEqualTo(LocalDate.of(2026, 9, 5));
        assertThat(report.feed().refusalStreak()).isEqualTo(2);
    }

    @Test
    void 마지막_급여를_먹었으면_연속_안_먹음은_0이다() {
        HealthReport report = HealthReportService.summarize(MANGO, FROM, TO, List.of(
                feed(LocalDate.of(2026, 9, 1), true),
                feed(LocalDate.of(2026, 9, 5), false)), List.of());

        assertThat(report.feed().refusalStreak()).isZero();
    }

    @Test
    void 탈피_요약은_블루_단계를_따로_세고_마지막_탈피일에서_뺀다() {
        HealthReport report = HealthReportService.summarize(MANGO, FROM, TO, List.of(
                shed(LocalDate.of(2026, 9, 1), ShedStage.SHED, true),
                shed(LocalDate.of(2026, 9, 28), ShedStage.SHED, false),
                shed(LocalDate.of(2026, 10, 25), ShedStage.BLUE, null)), List.of());

        assertThat(report.shed().complete()).isEqualTo(1);
        assertThat(report.shed().partial()).isEqualTo(1);
        assertThat(report.shed().sheds()).isEqualTo(2);
        assertThat(report.shed().blue()).isEqualTo(1);
        assertThat(report.shed().lastShedDate()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    void 진료_기록은_기간_안의_것만_양끝_포함해서_담는다() {
        HealthReport report = HealthReportService.summarize(MANGO, FROM, TO, List.of(), List.of(
                visit(LocalDate.of(2026, 11, 11), "탈피 부전"),
                visit(LocalDate.of(2026, 8, 11), "검진"),
                visit(LocalDate.of(2026, 8, 10), "기간 밖")));

        assertThat(report.visits()).extracting(HealthReport.VisitEntry::diagnosis).containsExactly("탈피 부전", "검진");
    }

    @Test
    void 기록이_없으면_요약은_비어_있다() {
        HealthReport report = HealthReportService.summarize(MANGO, FROM, TO, List.of(), List.of());

        assertThat(report.weights()).isEmpty();
        assertThat(report.feed().total()).isZero();
        assertThat(report.feed().lastEatenDate()).isNull();
        assertThat(report.shed().lastShedDate()).isNull();
        assertThat(report.visits()).isEmpty();
    }

    @Test
    void 기간은_한국_날짜_오늘까지_months개월이고_내_개체가_아니면_ANIMAL_NOT_FOUND() {
        AnimalRepository animalRepository = mock(AnimalRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        // UTC 11-10 15:00 = KST 11-11 00:00
        Clock clock = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneOffset.UTC);
        HealthReportService service = new HealthReportService(CurrentUserStub.of(1L), animalRepository,
                logRepository, mock(VetVisitRepository.class), new HealthReportPdfWriter(), clock);
        given(animalRepository.findByIdAndUserId(12L, 1L)).willReturn(Optional.of(MANGO));
        given(logRepository.findAllByAnimalIdAndLogDateBetweenOrderByLogDateAscIdAsc(12L, FROM, TO))
                .willReturn(List.of(weight(LocalDate.of(2026, 9, 1), "48.0")));

        byte[] pdf = service.createReport(12L, 3);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThatThrownBy(() -> service.createReport(99L, 3))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ANIMAL_NOT_FOUND);
    }

    private static Log weight(LocalDate date, String grams) {
        return Log.create(MANGO, null, null, LogType.WEIGHT, date, null,
                new LogDetails(null, null, null, null, null, null, new BigDecimal(grams), null, null, null, null,
                        null));
    }

    private static Log feed(LocalDate date, boolean refused) {
        return Log.create(MANGO, null, null, LogType.FEED, date, null,
                new LogDetails("귀뚜라미", 5, refused, null, null, null, null, null, null, null, null, null));
    }

    private static Log shed(LocalDate date, ShedStage stage, Boolean complete) {
        return Log.create(MANGO, null, null, LogType.SHED, date, null,
                new LogDetails(null, null, null, null, stage, complete, null, null, null, null, null, null));
    }

    private static VetVisit visit(LocalDate date, String diagnosis) {
        return VetVisit.builder().animal(MANGO).hospital("특수동물병원").visitDate(date).diagnosis(diagnosis).build();
    }
}
