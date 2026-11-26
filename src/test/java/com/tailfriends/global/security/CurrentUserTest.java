package com.tailfriends.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class CurrentUserTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUser currentUser = new CurrentUser(userRepository);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void 인증된_사용자_ID를_돌려준다() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                7L, null, List.of(new SimpleGrantedAuthority("ROLE_KEEPER"))));
        given(userRepository.existsById(7L)).willReturn(true);

        assertThat(currentUser.id()).isEqualTo(7L);
    }

    @Test
    void 토큰은_유효해도_탈퇴해_사용자가_없으면_UNAUTHORIZED() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                7L, null, List.of(new SimpleGrantedAuthority("ROLE_KEEPER"))));
        given(userRepository.existsById(7L)).willReturn(false);

        assertThatThrownBy(currentUser::id)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 한_요청에서는_사용자가_있는지_한_번만_확인한다() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        authenticate(7L);
        given(userRepository.existsById(7L)).willReturn(true);

        currentUser.id();
        currentUser.id();

        verify(userRepository, times(1)).existsById(7L);
    }

    @Test
    void 요청이_달라지면_다시_확인한다() {
        authenticate(7L);
        given(userRepository.existsById(7L)).willReturn(true, false);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        currentUser.id();

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertThatThrownBy(currentUser::id)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 요청_밖에서는_부를_때마다_확인한다() {
        authenticate(7L);
        given(userRepository.existsById(7L)).willReturn(true);

        currentUser.id();
        currentUser.id();

        verify(userRepository, times(2)).existsById(7L);
    }

    @Test
    void 인증_정보가_없으면_UNAUTHORIZED() {
        assertThatThrownBy(currentUser::id)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 익명_사용자면_UNAUTHORIZED() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(currentUser::id)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private static void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_KEEPER"))));
    }
}
