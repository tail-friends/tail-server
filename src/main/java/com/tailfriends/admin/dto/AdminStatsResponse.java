package com.tailfriends.admin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영 대시보드. daily는 날짜(KST) 오름차순이고, 기록이 없는 날도 0으로 채운다.
 */
public record AdminStatsResponse(
        long totalUsers,
        long totalAnimals,
        List<Daily> daily
) {

    public record Daily(
            LocalDate date,
            long newUsers,
            long newAnimals,
            long aiAnalyses,
            long aiFailures
    ) {
    }
}
