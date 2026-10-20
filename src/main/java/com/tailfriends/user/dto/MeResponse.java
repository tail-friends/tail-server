package com.tailfriends.user.dto;

import com.tailfriends.user.domain.User;

/**
 * 로그인한 사용자 정보. email은 소셜 쪽에서 동의하지 않았으면 null이다.
 */
public record MeResponse(
        Long id,
        String nickname,
        String email,
        int points,
        long animalCount
) {

    public static MeResponse of(User user, long animalCount) {
        return new MeResponse(user.getId(), user.getNickname(), user.getEmail(), user.getPoints(), animalCount);
    }
}
