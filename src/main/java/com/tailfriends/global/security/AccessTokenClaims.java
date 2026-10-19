package com.tailfriends.global.security;

import com.tailfriends.user.domain.Role;

public record AccessTokenClaims(
        Long userId,
        Role role
) {
}
