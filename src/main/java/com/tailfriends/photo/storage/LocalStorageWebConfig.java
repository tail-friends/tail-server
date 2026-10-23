package com.tailfriends.photo.storage;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 서버 디스크에 저장한 사진을 /files/{key}로 내려준다. key는 추측할 수 없는 UUID라 로그인 없이 연다.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 폴더가 아직 없으면 toUri()가 끝에 /를 붙이지 않아 직접 붙인다.
        String location = Path.of(storageProperties.localPath()).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
