package com.tailfriends.user.dto;

import com.tailfriends.user.domain.Role;
import com.tailfriends.user.domain.User;

/**
 * 로그인한 사용자 정보. email은 소셜 쪽에서 동의하지 않았으면 null이다.
 * role은 요청 때의 DB 역할이다. 관리자 웹은 ADMIN이 아니면 권한 없음 화면으로 보낸다.
 */
public record MeResponse(
        Long id,
        String nickname,
        String email,
        int points,
        long animalCount,
        Role role
) {

    public static MeResponse of(User user, long animalCount) {
        return new MeResponse(user.getId(), user.getNickname(), user.getEmail(), user.getPoints(), animalCount,
                user.getRole());
    }
}
