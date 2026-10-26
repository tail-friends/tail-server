package com.tailfriends.log.repository;

/**
 * 급여 기록의 먹이와 개수. 다음 급여를 미리 채울 때 쓴다.
 */
public record FedFood(
        String food,
        Integer count
) {
}
