package com.tailfriends.animal.domain;

import com.tailfriends.species.domain.Shape;
import java.math.BigDecimal;

/**
 * 캐릭터 모양 값. 색은 #RRGGBB, head·tail·patternDensity는 0~1 (없으면 null).
 */
public record CharacterLook(
        RenderStyle renderStyle,
        Shape shape,
        PatternType patternType,
        String colorBody,
        String colorPattern,
        String colorBelly,
        String colorEye,
        BigDecimal headSize,
        BigDecimal tailWidth,
        BigDecimal patternDensity
) {
}
