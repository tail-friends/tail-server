package com.tailfriends.log.repository;

import java.time.LocalDate;

/**
 * 개체별 마지막 급여일 ("안 먹음" 기록 제외).
 */
public record LastFedDate(
        Long animalId,
        LocalDate date
) {
}
