package com.tailfriends.animal.dto;

import com.tailfriends.species.domain.Home;
import java.util.Locale;

/**
 * home은 소문자. (개체 상세의 character.home과 같은 형식) 사진 테마면 home은 null이고 enclosureId가 그 사육장.
 */
public record AnimalHomeResponse(String home, Long enclosureId) {

    public static AnimalHomeResponse from(Home home) {
        return new AnimalHomeResponse(home.name().toLowerCase(Locale.ROOT), null);
    }

    public static AnimalHomeResponse photoTheme(Long enclosureId) {
        return new AnimalHomeResponse(null, enclosureId);
    }
}
