package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tailfriends.auth.client.OAuthClient;
import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.dto.OAuthLoginRequest;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.Provider;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuthServiceTest {

    private final OAuthClient kakao = mock(OAuthClient.class);
    private final SignInService signInService = mock(SignInService.class);
    private final OAuthService oauthService = new OAuthService(List.of(kakao), signInService);

    @Test
    void 요청한_로그인_방식의_클라이언트로_사용자_정보를_가져와_로그인한다() {
        OAuthUserInfo userInfo = new OAuthUserInfo("4215", "망고집사", null);
        OAuthLoginResponse expected = new OAuthLoginResponse("access", "refresh", true,
                new OAuthLoginResponse.LoginUser(1L, "망고집사"));
        given(kakao.provider()).willReturn(Provider.KAKAO);
        given(kakao.fetchUser("auth-code", "https://app.example/callback")).willReturn(userInfo);
        given(signInService.signIn(Provider.KAKAO, userInfo)).willReturn(expected);

        OAuthLoginResponse response = oauthService.login(
                new OAuthLoginRequest(Provider.KAKAO, "auth-code", "https://app.example/callback"));

        assertThat(response).isEqualTo(expected);
    }

    @Test
    void 지원하지_않는_로그인_방식이면_INVALID_INPUT() {
        given(kakao.provider()).willReturn(Provider.KAKAO);

        assertThatThrownBy(() -> oauthService.login(
                new OAuthLoginRequest(Provider.GOOGLE, "auth-code", "https://app.example/callback")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
