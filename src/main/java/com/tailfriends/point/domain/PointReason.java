package com.tailfriends.point.domain;

import com.tailfriends.log.domain.LogType;
import java.util.Optional;

/**
 * 포인트 내역의 이유. 기록 적립은 종류마다 하나씩이다. PURCHASE는 상점이 생기면 추가한다.
 */
public enum PointReason {
    LOG_FEED,
    LOG_WEIGHT,
    LOG_SHED,
    LOG_CLEAN;

    /**
     * 적립하는 기록 종류면 그 이유. 온습도·메모는 적립하지 않는다.
     */
    public static Optional<PointReason> ofLog(LogType type) {
        return switch (type) {
            case FEED -> Optional.of(LOG_FEED);
            case WEIGHT -> Optional.of(LOG_WEIGHT);
            case SHED -> Optional.of(LOG_SHED);
            case CLEAN -> Optional.of(LOG_CLEAN);
            case ENV, NOTE -> Optional.empty();
        };
    }
}
