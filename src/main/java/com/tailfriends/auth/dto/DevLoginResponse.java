package com.tailfriends.auth.dto;

import com.tailfriends.user.domain.Role;

public record DevLoginResponse(
        Long userId,
        Role role,
        String accessToken,
        String refreshToken
) {
}
