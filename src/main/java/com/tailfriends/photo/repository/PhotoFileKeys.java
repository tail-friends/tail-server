package com.tailfriends.photo.repository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 사진 한 장의 저장소 파일 key. 엔티티를 불러오지 않고 파일만 지울 때 쓴다.
 */
public record PhotoFileKeys(
        String thumbnailKey,
        String originalKey
) {

    /**
     * 원본을 보관하지 않았으면 썸네일만.
     */
    public List<String> all() {
        return Stream.of(thumbnailKey, originalKey).filter(Objects::nonNull).toList();
    }
}
