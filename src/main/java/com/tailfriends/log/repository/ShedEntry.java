package com.tailfriends.log.repository;

import com.tailfriends.log.domain.ShedStage;
import java.time.LocalDate;

/**
 * 탈피 기록 하나. complete는 완탈 여부 (블루 단계면 false).
 */
public record ShedEntry(
        LocalDate date,
        ShedStage stage,
        Boolean complete
) {
}
