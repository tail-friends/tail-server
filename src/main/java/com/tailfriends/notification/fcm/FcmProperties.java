package com.tailfriends.notification.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param credentials Firebase 서비스 계정 키(JSON)를 Base64로 인코딩한 값. 환경 변수로만 넣는다.
 *                    비어 있으면 푸시를 보내지 않는다(로컬·테스트)
 */
@ConfigurationProperties(prefix = "fcm")
public record FcmProperties(
        String credentials
) {
}
