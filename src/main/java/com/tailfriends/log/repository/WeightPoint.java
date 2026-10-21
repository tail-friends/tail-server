package com.tailfriends.log.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 체중 기록 하나 (g).
 */
public record WeightPoint(
        LocalDate date,
        BigDecimal value
) {
}
