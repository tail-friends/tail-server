package com.tailfriends.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.service.PhotoFileRemover;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhotoFileRemover photoFileRemover;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CurrentUser currentUser;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void 현재_사용자() {
        given(currentUser.id()).willReturn(1L);
    }

    @Test
    void 탈퇴하면_사용자와_사진_파일을_지운다() {
        User user = User.builder().provider(Provider.KAKAO).providerId("1").nickname("망고집사").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.withdraw();

        verify(userRepository).delete(user);
        verify(photoFileRemover).removeFilesOfUserAfterCommit(1L);
    }

    @Test
    void 이미_없는_사용자면_UNAUTHORIZED() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
        verify(userRepository, never()).delete(any());
        verify(photoFileRemover, never()).removeFilesOfUserAfterCommit(any());
    }
}
