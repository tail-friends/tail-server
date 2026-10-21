package com.tailfriends.animal.domain;

import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.repository.ShedEntry;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 탈피 요약. 기능명세서 4-4.
 *
 * @param lastDate        마지막 탈피일 (블루 단계 제외). 없으면 null
 * @param complete        마지막 탈피가 완탈인지. 없으면 null
 * @param avgIntervalDays 탈피 기록이 2개 이상이면 최근 5번 간격의 평균(반올림). 아니면 null
 * @param predictedDate   탈피 기록이 3개 이상이면 lastDate + 평균 간격. 아니면 null
 * @param inBluePhase     가장 최근 탈피 기록이 블루 단계인지. 블루 단계는 예정일보다 우선한다
 */
public record ShedSummary(
        LocalDate lastDate,
        Boolean complete,
        Integer avgIntervalDays,
        LocalDate predictedDate,
        boolean inBluePhase
) {

    static final int INTERVAL_COUNT = 5;
    static final int MIN_FOR_AVERAGE = 2;
    static final int MIN_FOR_PREDICTION = 3;

    /**
     * 조회해야 할 최근 기록 수. 간격 5개를 구하려면 탈피 6번이 필요하고, 사이에 블루 단계 기록이 섞여 있을 수 있다.
     */
    public static final int RECENT_LOGS = 20;

    /**
     * @param entries 탈피 기록, 최신순 (블루 단계 포함)
     */
    public static ShedSummary from(List<ShedEntry> entries) {
        boolean inBluePhase = !entries.isEmpty() && entries.get(0).stage() == ShedStage.BLUE;
        List<ShedEntry> sheds = entries.stream()
                .filter(entry -> entry.stage() == ShedStage.SHED)
                .toList();
        if (sheds.isEmpty()) {
            return new ShedSummary(null, null, null, null, inBluePhase);
        }

        ShedEntry last = sheds.get(0);
        Integer avgIntervalDays = sheds.size() >= MIN_FOR_AVERAGE ? averageInterval(sheds) : null;
        LocalDate predictedDate = sheds.size() >= MIN_FOR_PREDICTION && avgIntervalDays != null
                ? last.date().plusDays(avgIntervalDays)
                : null;
        return new ShedSummary(last.date(), Boolean.TRUE.equals(last.complete()), avgIntervalDays, predictedDate,
                inBluePhase);
    }

    private static Integer averageInterval(List<ShedEntry> sheds) {
        int count = Math.min(INTERVAL_COUNT, sheds.size() - 1);
        long total = 0;
        for (int i = 0; i < count; i++) {
            total += ChronoUnit.DAYS.between(sheds.get(i + 1).date(), sheds.get(i).date());
        }
        return (int) Math.round((double) total / count);
    }
}
