package com.tailfriends.photo.storage;

/**
 * 사진 파일 저장소. 나중에 S3 호환 저장소로 옮기면 구현만 바꾼다. DB에는 key만 둔다.
 */
public interface PhotoStorage {

    void save(String key, byte[] bytes);

    void delete(String key);

    String urlOf(String key);
}
