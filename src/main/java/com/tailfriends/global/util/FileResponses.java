package com.tailfriends.global.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 파일을 내려주는 API(CSV, PDF)의 응답. 본문이 파일이라 ApiResponse로 감싸지 않는다.
 * 실패는 다른 API처럼 BusinessException을 던져 GlobalExceptionHandler가 JSON으로 돌려준다.
 */
public final class FileResponses {

    private FileResponses() {
    }

    /**
     * 내려받기(attachment) 응답. 파일 이름은 영문·숫자로 쓴다(인코딩 없이 그대로 헤더에 들어간다).
     */
    public static ResponseEntity<byte[]> attachment(byte[] body, MediaType contentType, String filename) {
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename)
                        .build()
                        .toString())
                .body(body);
    }
}
