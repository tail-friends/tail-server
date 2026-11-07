package com.tailfriends.notification.dto;

import com.tailfriends.notification.domain.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushTokenRequest(
        @NotBlank @Size(max = 512) String token,
        @NotNull PushPlatform platform
) {
}
