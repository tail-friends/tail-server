package com.tailfriends.photo.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * 서버 디스크에 저장한다. 파일은 LocalStorageWebConfig가 /files/** 로 내려준다.
 */
@Component
public class LocalPhotoStorage implements PhotoStorage {

    private final Path root;
    private final String publicUrl;

    public LocalPhotoStorage(StorageProperties properties) {
        this.root = Path.of(properties.localPath()).toAbsolutePath().normalize();
        this.publicUrl = properties.publicUrl().replaceAll("/+$", "");
    }

    @Override
    public void save(String key, byte[] bytes) {
        Path path = resolve(key);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 저장하지 못했어요: " + key, e);
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 읽지 못했어요: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 지우지 못했어요: " + key, e);
        }
    }

    @Override
    public String urlOf(String key) {
        return publicUrl + "/" + key;
    }

    /**
     * key는 서버가 만들지만, 저장소 밖 경로를 가리키지 못하게 한 번 더 막는다.
     */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 사진 key: " + key);
        }
        return path;
    }
}
