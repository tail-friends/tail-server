package com.tailfriends.enclosure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.domain.Enclosure;
import com.tailfriends.enclosure.domain.EnclosureObject;
import java.util.List;
import java.util.Locale;

/**
 * 사육장 배경. 기본 테마는 type·theme, 사진 테마는 type·photoId·색(상세에서는 objects도)만 내보낸다.
 */
public sealed interface BackgroundResponse {

    BackgroundType type();

    /**
     * 목록용. 사진 테마의 objects는 넣지 않는다.
     */
    static BackgroundResponse from(Enclosure enclosure) {
        return of(enclosure, null);
    }

    /**
     * 상세용. 사진 테마면 objects를 넣는다.
     */
    static BackgroundResponse of(Enclosure enclosure, List<EnclosureObject> objects) {
        if (enclosure.isBuiltIn()) {
            return new BuiltIn(BackgroundType.BUILT_IN, enclosure.getTheme().name().toLowerCase(Locale.ROOT));
        }
        return new Photo(BackgroundType.PHOTO, enclosure.getPhotoId(), enclosure.getFloorColor(),
                enclosure.getWallColor(), objects == null ? null : objects.stream().map(ObjectResponse::from).toList());
    }

    /**
     * theme은 기본 테마의 소문자 이름(개체의 home과 같은 값).
     */
    record BuiltIn(BackgroundType type, String theme) implements BackgroundResponse {
    }

    /**
     * photoId는 사진을 지웠으면 null.
     */
    record Photo(
            BackgroundType type,
            Long photoId,
            String floorColor,
            String wallColor,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<ObjectResponse> objects
    ) implements BackgroundResponse {
    }

    record ObjectResponse(String type, double x, double y) {

        static ObjectResponse from(EnclosureObject object) {
            return new ObjectResponse(
                    object.getType().name(), object.getX().doubleValue(), object.getY().doubleValue());
        }
    }
}
