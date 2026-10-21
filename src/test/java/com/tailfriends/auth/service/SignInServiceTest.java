package com.tailfriends.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.TokenResponse;
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
    private final SignInService signInService = new SignInService(userRepository, tokenService);

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
