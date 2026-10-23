package com.tailfriends.global.support;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.global.security.CurrentUser;

/**
 * 서비스 단위 테스트용. 항상 같은 사용자 ID를 돌려주는 CurrentUser.
 */
public final class CurrentUserStub {

    private CurrentUserStub() {
    }

    public static CurrentUser of(long userId) {
        CurrentUser currentUser = mock(CurrentUser.class);
        given(currentUser.id()).willReturn(userId);
        return currentUser;
    }
}
