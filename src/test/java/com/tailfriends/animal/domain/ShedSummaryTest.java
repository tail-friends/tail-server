package com.tailfriends.animal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.repository.ShedEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShedSummaryTest {

    @Test
    void 탈피_기록이_없으면_모두_비어_있다() {
        assertThat(ShedSummary.from(List.of())).isEqualTo(new ShedSummary(null, null, null, null, false));
    }

    @Test
    void 탈피가_1번이면_마지막_탈피일만_있다() {
        ShedSummary summary = ShedSummary.from(List.of(shed("2026-09-22", false)));

        assertThat(summary).isEqualTo(new ShedSummary(LocalDate.parse("2026-09-22"), false, null, null, false));
    }

    @Test
    void 탈피가_2번이면_평균_주기만_있고_예정일은_없다() {
        ShedSummary summary = ShedSummary.from(List.of(shed("2026-09-22", true), shed("2026-08-22", true)));

        assertThat(summary.avgIntervalDays()).isEqualTo(31);
        assertThat(summary.predictedDate()).isNull();
    }

    @Test
    void 탈피가_3번_이상이면_예정일을_계산한다() {
        ShedSummary summary = ShedSummary.from(List.of(
                shed("2026-09-22", true), shed("2026-08-20", true), shed("2026-07-21", true)));

        // 간격 33일, 30일 → 평균 31.5 → 반올림 32
        assertThat(summary.avgIntervalDays()).isEqualTo(32);
        assertThat(summary.predictedDate()).isEqualTo(LocalDate.parse("2026-10-24"));
    }

    @Test
    void 평균은_최근_5번_간격만_본다() {
        ShedSummary summary = ShedSummary.from(List.of(
                shed("2026-06-30", true), shed("2026-06-20", true), shed("2026-06-10", true),
                shed("2026-05-31", true), shed("2026-05-21", true), shed("2026-05-11", true),
                shed("2026-01-01", true)));

        assertThat(summary.avgIntervalDays()).isEqualTo(10);
    }

    @Test
    void 블루_단계는_평균에서_빼고_가장_최근이_블루면_블루_단계다() {
        ShedSummary summary = ShedSummary.from(List.of(
                blue("2026-10-20"), shed("2026-09-22", true), blue("2026-09-18"),
                shed("2026-08-22", true), shed("2026-07-23", true)));

        assertThat(summary.inBluePhase()).isTrue();
        assertThat(summary.lastDate()).isEqualTo(LocalDate.parse("2026-09-22"));
        assertThat(summary.avgIntervalDays()).isEqualTo(31);
    }

    @Test
    void 블루_단계_뒤에_탈피했으면_블루_단계가_아니다() {
        ShedSummary summary = ShedSummary.from(List.of(shed("2026-10-21", true), blue("2026-10-15")));

        assertThat(summary.inBluePhase()).isFalse();
    }

    private static ShedEntry shed(String date, boolean complete) {
        return new ShedEntry(LocalDate.parse(date), ShedStage.SHED, complete);
    }

    private static ShedEntry blue(String date) {
        return new ShedEntry(LocalDate.parse(date), ShedStage.BLUE, false);
    }
}
