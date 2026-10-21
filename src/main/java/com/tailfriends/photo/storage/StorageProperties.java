package com.tailfriends.photo.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param localPath 사진을 저장할 서버 디스크 경로
 * @param publicUrl 사진 주소 앞부분. 사진 주소는 {publicUrl}/{key}
 */
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        String localPath,
        String publicUrl
) {
}
