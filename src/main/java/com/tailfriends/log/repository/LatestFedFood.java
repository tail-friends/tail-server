package com.tailfriends.log.repository;

/**
 * 개체별 가장 최근에 먹은 급여 기록의 먹이와 개수. 다음 급여를 미리 채울 때 쓴다.
 */
public record LatestFedFood(
        Long animalId,
        String food,
        Integer count
) {
}
