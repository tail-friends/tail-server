package com.tailfriends.user.dto;

import com.tailfriends.user.domain.ConsentType;
import java.util.Map;

public record ConsentResponse(
        boolean privacy,
        boolean photoAnalysis,
        boolean keepOriginal,
        boolean notification
) {

    /**
     * 한 번도 답하지 않은 항목은 false.
     */
    public static ConsentResponse from(Map<ConsentType, Boolean> agreed) {
        return new ConsentResponse(
                agreed.getOrDefault(ConsentType.PRIVACY, false),
                agreed.getOrDefault(ConsentType.PHOTO_ANALYSIS, false),
                agreed.getOrDefault(ConsentType.KEEP_ORIGINAL, false),
                agreed.getOrDefault(ConsentType.NOTIFICATION, false));
    }
}
