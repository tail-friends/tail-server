package com.tailfriends.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않아요"),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "JPG·PNG 사진만 20MB까지 올릴 수 있어요"),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요"),
    OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했어요. 다시 시도해 주세요"),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요"),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없어요"),
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "개체를 찾을 수 없어요"),
    SPECIES_NOT_FOUND(HttpStatus.NOT_FOUND, "종을 찾을 수 없어요"),
    LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없어요"),
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "경고를 찾을 수 없어요"),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "사진을 찾을 수 없어요"),

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요"),

    // 409
    DUPLICATE_CLIENT_ID(HttpStatus.CONFLICT, "이미 저장된 기록이에요"),
    NOT_ENOUGH_POINTS(HttpStatus.CONFLICT, "포인트가 부족해요"),

    // 422
    NO_REPTILE_IN_PHOTO(HttpStatus.UNPROCESSABLE_ENTITY, "사진에서 파충류를 찾지 못했어요"),

    // 429
    AI_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘 AI 분석 횟수를 모두 사용했어요"),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요"),

    // 503
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "지금은 AI 분석을 할 수 없어요. 잠시 후 다시 시도해 주세요");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
