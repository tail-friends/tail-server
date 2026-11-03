package com.tailfriends.enclosure.dto;

import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import java.util.Locale;

/**
 * theme은 기본 테마의 소문자 이름(개체의 home과 같은 값). 사진 테마 필드는 사진 테마를 만들 때 추가한다.
 */
public record BackgroundResponse(
        BackgroundType type,
        String theme
) {

    public static BackgroundResponse from(Enclosure enclosure) {
        String theme = enclosure.getTheme() == null ? null : enclosure.getTheme().name().toLowerCase(Locale.ROOT);
        return new BackgroundResponse(enclosure.getBackgroundType(), theme);
    }
}
