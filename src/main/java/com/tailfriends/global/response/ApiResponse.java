package com.tailfriends.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.tailfriends.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"success", "data", "error"})
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return error(errorCode, message, null);
    }

    /**
     * 실패해도 data를 함께 돌려줘야 하는 경우에만 쓴다. (예: DUPLICATE_CLIENT_ID)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(false, data, new ErrorResponse(errorCode.getCode(), message));
    }
}
