package com.tailfriends.animal.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 체중 요약 (g).
 *
 * @param latest 가장 최근 체중. 기록이 없으면 null
 * @param diff   직전 체중 기록 대비 증감. 기록이 2개 미만이면 null
 */
public record WeightSummaryResponse(
        BigDecimal latest,
        BigDecimal diff
) {

    /**
     * @param latestWeights 최근 체중, 최신순 (2개까지 본다)
     */
    public static WeightSummaryResponse from(List<BigDecimal> latestWeights) {
        if (latestWeights.isEmpty()) {
            return new WeightSummaryResponse(null, null);
        }
        BigDecimal latest = latestWeights.get(0);
        BigDecimal diff = latestWeights.size() < 2 ? null : latest.subtract(latestWeights.get(1));
        return new WeightSummaryResponse(latest, diff);
    }
}
