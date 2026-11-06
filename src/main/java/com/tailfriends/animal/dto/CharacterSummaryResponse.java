package com.tailfriends.animal.dto;

import com.tailfriends.animal.domain.AnimalCharacter;
import com.tailfriends.species.domain.Home;
import java.util.Locale;

/**
 * 개체 상세의 캐릭터. 노션 명세 형식(shape, patternType, colors, home, enclosureId).
 * home은 평소 사육장 테마. 개체에 지정한 값이 없으면 종의 기본 사육장, 직접 입력한 종이면 null.
 * 평소 사육장이 사진 테마면 enclosureId가 그 사육장이고 home은 null.
 */
public record CharacterSummaryResponse(
        String shape,
        String patternType,
        CharacterResponse.Colors colors,
        String home,
        Long enclosureId
) {

    public static CharacterSummaryResponse of(AnimalCharacter character, Home home, Long enclosureId) {
        CharacterResponse saved = CharacterResponse.from(character);
        return new CharacterSummaryResponse(
                saved.shape(),
                saved.patternType(),
                saved.colors(),
                home == null ? null : home.name().toLowerCase(Locale.ROOT),
                enclosureId);
    }
}
