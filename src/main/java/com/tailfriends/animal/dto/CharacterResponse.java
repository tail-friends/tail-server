package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.AnimalCharacter;
import com.tailfriends.animal.domain.RenderStyle;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * 저장된 캐릭터. shape, patternType은 명세 예시처럼 소문자로 내려준다.
 */
public record CharacterResponse(
        RenderStyle renderStyle,
        String shape,
        String patternType,
        Colors colors,
        Extra extra
) {

    public static CharacterResponse from(AnimalCharacter character) {
        return new CharacterResponse(
                character.getRenderStyle(),
                character.getShape().name().toLowerCase(Locale.ROOT),
                character.getPatternType().name().toLowerCase(Locale.ROOT),
                new Colors(character.getColorBody(), character.getColorPattern(),
                        character.getColorBelly(), character.getColorEye()),
                new Extra(character.getHeadSize(), character.getTailWidth(), character.getPatternDensity()));
    }

    public record Colors(
            String body,
            String pattern,
            String belly,
            String eye
    ) {
    }

    public record Extra(
            BigDecimal headSize,
            BigDecimal tailWidth,
            BigDecimal patternDensity
    ) {
    }
}
