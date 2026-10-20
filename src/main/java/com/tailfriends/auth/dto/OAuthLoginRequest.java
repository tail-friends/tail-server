package com.tailfriends.auth.dto;

import com.tailfriends.user.domain.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * redirectUri는 앱이 인가 코드를 받을 때 쓴 값과 같아야 한다. (소셜 서비스가 대조한다)
 */
public record OAuthLoginRequest(
        @NotNull Provider provider,
        @NotBlank String code,
        @NotBlank String redirectUri
) {
}
