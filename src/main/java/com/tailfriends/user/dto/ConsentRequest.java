package com.tailfriends.user.dto;

import com.tailfriends.user.domain.ConsentType;
import jakarta.validation.constraints.NotNull;
import java.util.EnumMap;
import java.util.Map;

/**
 * 네 항목을 모두 보낸다. 필수 항목이 false인지는 서비스에서 확인한다.
 */
public record ConsentRequest(
        @NotNull Boolean privacy,
        @NotNull Boolean photoAnalysis,
        @NotNull Boolean keepOriginal,
        @NotNull Boolean notification
) {

    public Map<ConsentType, Boolean> toMap() {
        Map<ConsentType, Boolean> map = new EnumMap<>(ConsentType.class);
        map.put(ConsentType.PRIVACY, privacy);
        map.put(ConsentType.PHOTO_ANALYSIS, photoAnalysis);
        map.put(ConsentType.KEEP_ORIGINAL, keepOriginal);
        map.put(ConsentType.NOTIFICATION, notification);
        return map;
    }
}
