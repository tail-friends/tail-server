package com.tailfriends.user.domain;

public enum Provider {
    KAKAO,
    GOOGLE,
    APPLE,
    /** 로컬 테스트 로그인 전용 (dev.login.enabled). 운영에서는 생기지 않는다. */
    DEV
}
