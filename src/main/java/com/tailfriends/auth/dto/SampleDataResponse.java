package com.tailfriends.auth.dto;

import java.util.List;

/**
 * 샘플 데이터로 만든 개체 ID와 기록 수.
 */
public record SampleDataResponse(
        List<Long> animalIds,
        int logCount
) {
}
