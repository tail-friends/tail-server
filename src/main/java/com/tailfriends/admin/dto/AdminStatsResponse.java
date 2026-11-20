package com.tailfriends.admin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영 대시보드. daily는 날짜(KST) 오름차순이고, 기록이 없는 날도 0으로 채운다.
 * speciesCounts는 종별 등록 개체 수로 많은 순이다. 합은 totalAnimals와 같다.
 */
public record AdminStatsResponse(
        long totalUsers,
        long totalAnimals,
        List<Daily> daily,
        List<SpeciesCount> speciesCounts
) {

    /**
     * @param speciesId 직접 입력한 종을 묶은 항목이면 null (name은 "직접 입력")
     */
    public record SpeciesCount(
            Long speciesId,
            String name,
            long count
    ) {
    }

    public record Daily(
            LocalDate date,
            long newUsers,
            long newAnimals,
            long aiAnalyses,
            long aiFailures
    ) {
    }
}
