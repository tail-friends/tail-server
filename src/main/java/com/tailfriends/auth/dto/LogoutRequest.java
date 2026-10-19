package com.tailfriends.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * pushToken은 push_tokens 테이블(출시 후)이 생기면 이 기기의 푸시 토큰을 지우는 데 쓴다.
 */
public record LogoutRequest(
        @NotBlank String refreshToken,
        String pushToken
) {
}
