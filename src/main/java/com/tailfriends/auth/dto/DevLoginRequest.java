package com.tailfriends.auth.dto;

import com.tailfriends.user.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 같은 name이면 같은 사용자다. role을 보내면 그 권한으로 바꾼다. (관리자 웹 테스트는 ADMIN)
 */
public record DevLoginRequest(
        @NotBlank @Size(max = 30) String name,
        Role role
) {
}
