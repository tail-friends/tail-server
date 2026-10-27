package com.tailfriends.photo.ai;

import java.math.BigDecimal;

/**
 * AI가 사진에서 읽은 캐릭터 값. reptile이 false면 나머지 값은 쓰지 않는다.
 * shape·patternType은 소문자, 색은 대문자 #RRGGBB, extra는 0~1 소수 둘째 자리.
 */
public record CharacterAnalysis(
        boolean reptile,
        String shape,
        String patternType,
        Colors colors,
        Extra extra,
        String guess
) {

    public record Colors(String body, String pattern, String belly, String eye) {
    }

    public record Extra(BigDecimal headSize, BigDecimal tailWidth, BigDecimal patternDensity) {
    }

    public static CharacterAnalysis notReptile() {
        return new CharacterAnalysis(false, null, null, null, null, null);
    }
}
