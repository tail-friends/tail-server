package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.domain.UserOAuthToken;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.repository.UserOAuthTokenRepository;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SignInServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final UserOAuthTokenRepository userOAuthTokenRepository = mock(UserOAuthTokenRepository.class);
    private final OAuthTokenCipher cipher = new OAuthTokenCipher("dGVzdC1vYXV0aC10b2tlbi1lbmNyeXB0aW9uLWtleSE=");
    private final SignInService signInService = new SignInService(userRepository, tokenService,
            userOAuthTokenRepository, cipher);

    @Test
    void 애플_refresh_token을_받으면_암호화해_저장한다() {
        given(tokenService.issue(any())).willReturn(new TokenResponse("access", "refresh"));
        given(userRepository.findByProviderAndProviderId(Provider.APPLE, "001234")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(userOAuthTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        signInService.signIn(Provider.APPLE, new OAuthUserInfo("001234", null, null, "apple-refresh"));

        ArgumentCaptor<UserOAuthToken> saved = ArgumentCaptor.forClass(UserOAuthToken.class);
        verify(userOAuthTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(Provider.APPLE);
        assertThat(saved.getValue().getRefreshToken()).isNotEqualTo("apple-refresh");
        assertThat(cipher.decrypt(saved.getValue().getRefreshToken())).isEqualTo("apple-refresh");
    }

    @Test
    void 이미_저장한_애플_토큰이_있으면_새_토큰으로_바꾼다() {
        given(tokenService.issue(any())).willReturn(new TokenResponse("access", "refresh"));
        User existing = withId(User.builder().provider(Provider.APPLE).providerId("001234").nickname("사육사").build(), 3L);
        UserOAuthToken token = UserOAuthToken.of(existing, Provider.APPLE, cipher.encrypt("old-refresh"));
        given(userRepository.findByProviderAndProviderId(Provider.APPLE, "001234")).willReturn(Optional.of(existing));
        given(userOAuthTokenRepository.findByUserId(3L)).willReturn(Optional.of(token));

        signInService.signIn(Provider.APPLE, new OAuthUserInfo("001234", null, null, "new-refresh"));

        assertThat(cipher.decrypt(token.getRefreshToken())).isEqualTo("new-refresh");
        verify(userOAuthTokenRepository, never()).save(any());
    }

    @Test
    void 소셜_refresh_token이_없으면_저장하지_않는다() {
        given(tokenService.issue(any())).willReturn(new TokenResponse("access", "refresh"));
        given(userRepository.findByProviderAndProviderId(any(), any())).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        signInService.signIn(Provider.KAKAO, new OAuthUserInfo("4215", "망고집사", null));

        verify(userOAuthTokenRepository, never()).save(any());
    }

    @Test
    void 처음_보는_소셜_계정이면_가입시키고_isNewUser는_true() {
        given(userRepository.findByProviderAndProviderId(Provider.KAKAO, "4215")).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(tokenService.issue(any())).willReturn(new TokenResponse("access", "refresh"));

        OAuthLoginResponse response = signInService.signIn(Provider.KAKAO,
                new OAuthUserInfo("4215", "망고집사", "keeper@example.com"));

        User saved = captureSaved();
        assertThat(saved.getProviderId()).isEqualTo("4215");
        assertThat(saved.getEmail()).isEqualTo("keeper@example.com");
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.user()).isEqualTo(new OAuthLoginResponse.LoginUser(1L, "망고집사"));
        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void 이미_가입한_계정이면_새로_만들지_않고_isNewUser는_false() {
        User existing = withId(User.builder().provider(Provider.GOOGLE).providerId("1098765").nickname("기존 닉네임").build(), 3L);
        given(userRepository.findByProviderAndProviderId(Provider.GOOGLE, "1098765")).willReturn(Optional.of(existing));
        given(tokenService.issue(existing)).willReturn(new TokenResponse("access", "refresh"));

        OAuthLoginResponse response = signInService.signIn(Provider.GOOGLE,
                new OAuthUserInfo("1098765", "바뀐 구글 이름", null));

        verify(userRepository, never()).save(any());
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.user()).isEqualTo(new OAuthLoginResponse.LoginUser(3L, "기존 닉네임"));
    }

    @Test
    void 닉네임이_없으면_기본_닉네임으로_가입한다() {
        given(userRepository.findByProviderAndProviderId(any(), any())).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(tokenService.issue(any())).willReturn(new TokenResponse("access", "refresh"));

        signInService.signIn(Provider.KAKAO, new OAuthUserInfo("4215", "  ", null));

        assertThat(captureSaved().getNickname()).isEqualTo(SignInService.DEFAULT_NICKNAME);
    }

    @Test
    void 닉네임이_30자를_넘으면_30자까지만_쓴다() {
        given(userRepository.findByProviderAndProviderId(any(), any())).willReturn(Optional.empty());
        given(userRepository.save(any())).willAnswer(invocation -> withId(invocation.getArgument(0), 1L));
        given(tokenService.issue(any())).willReturn(new TokenResponse("access", "refresh"));

        signInService.signIn(Provider.KAKAO, new OAuthUserInfo("4215", "🦎".repeat(31), null));

        assertThat(captureSaved().getNickname()).isEqualTo("🦎".repeat(30));
    }

    private User captureSaved() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
