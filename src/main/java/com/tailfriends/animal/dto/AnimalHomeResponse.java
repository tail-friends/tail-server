package com.tailfriends.animal.dto;

import com.tailfriends.species.domain.Home;
import java.util.Locale;

/**
 * home은 소문자. (개체 상세의 character.home과 같은 형식)
 */
public record AnimalHomeResponse(String home) {

    public static AnimalHomeResponse from(Home home) {
        return new AnimalHomeResponse(home.name().toLowerCase(Locale.ROOT));
    }
}
