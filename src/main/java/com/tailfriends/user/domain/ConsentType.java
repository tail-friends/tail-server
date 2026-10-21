package com.tailfriends.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConsentType {

    PRIVACY(true),
    PHOTO_ANALYSIS(true),
    KEEP_ORIGINAL(false),
    NOTIFICATION(false);

    /**
     * 필수 항목은 동의해야 앱을 쓸 수 있다.
     */
    private final boolean required;
}
