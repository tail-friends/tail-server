package com.tailfriends.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.user.domain.Role;
import com.tailfriends.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AdminAuthorizationManagerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminAuthorizationManager manager = new AdminAuthorizationManager(userRepository);

    @Test
    void DB_역할이_ADMIN이면_허용한다() {
        given(userRepository.findRoleById(7L)).willReturn(Optional.of(Role.ADMIN));

        assertThat(granted(user(7L, "KEEPER"))).isTrue();
    }

    @Test
    void 토큰은_ADMIN이어도_DB_역할이_KEEPER면_거부한다() {
        given(userRepository.findRoleById(7L)).willReturn(Optional.of(Role.KEEPER));

        assertThat(granted(user(7L, "ADMIN"))).isFalse();
    }

    @Test
    void 사용자가_없으면_거부한다() {
        given(userRepository.findRoleById(7L)).willReturn(Optional.empty());

        assertThat(granted(user(7L, "ADMIN"))).isFalse();
    }

    @Test
    void 로그인하지_않았으면_DB를_보지_않고_거부한다() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(granted(anonymous)).isFalse();
        assertThat(granted(null)).isFalse();
        verify(userRepository, never()).findRoleById(any());
    }

    private boolean granted(Authentication authentication) {
        return manager.check(() -> authentication, null).isGranted();
    }

    private static Authentication user(long userId, String tokenRole) {
        return UsernamePasswordAuthenticationToken.authenticated(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + tokenRole)));
    }
}
