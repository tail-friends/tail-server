package com.tailfriends.log.domain;

import java.math.BigDecimal;

/**
 * 기록 종류별 값. 기록의 종류에 해당하는 값만 쓰고 나머지는 무시한다.
 *
 * <ul>
 *   <li>FEED: food, count, refused, dusted</li>
 *   <li>SHED: stage, complete</li>
 *   <li>WEIGHT: weight (g)</li>
 *   <li>CLEAN: full, stool</li>
 *   <li>ENV: hot, cool (℃), humidity (%)</li>
 * </ul>
 */
public record LogDetails(
        String food,
        Integer count,
        Boolean refused,
        Boolean dusted,
        ShedStage stage,
        Boolean complete,
        BigDecimal weight,
        Boolean full,
        Stool stool,
        BigDecimal hot,
        BigDecimal cool,
        Integer humidity
) {
}
