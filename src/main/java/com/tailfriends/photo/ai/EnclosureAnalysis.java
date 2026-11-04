package com.tailfriends.photo.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI가 사육장 사진에서 읽은 값. enclosure가 false면 나머지 값은 쓰지 않는다.
 * 색은 대문자 #RRGGBB, 장식 위치는 0~1 소수 셋째 자리(왼쪽 위가 0, 0).
 */
public record EnclosureAnalysis(
        boolean enclosure,
        String floorColor,
        String wallColor,
        List<DetectedObject> objects
) {

    public record DetectedObject(EnclosureObjectType type, BigDecimal x, BigDecimal y) {
    }

    public static EnclosureAnalysis notEnclosure() {
        return new EnclosureAnalysis(false, null, null, List.of());
    }
}
