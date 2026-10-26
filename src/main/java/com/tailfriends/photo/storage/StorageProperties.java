package com.tailfriends.photo.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param localPath 사진을 저장할 서버 디스크 경로
 * @param publicUrl 사진 주소 앞부분. 사진 주소는 {publicUrl}/{key}
 * @param dailyUploadLimit 사용자당 하루(KST) 사진 업로드 수. 디스크가 차지 않게 막는다
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String localPath,
        String publicUrl,
        int dailyUploadLimit
) {
}
