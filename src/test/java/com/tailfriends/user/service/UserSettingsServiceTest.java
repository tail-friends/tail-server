package com.tailfriends.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.user.domain.AlertRules;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.TempUnit;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.domain.UserSettings;
import com.tailfriends.user.dto.ConsentResponse;
import com.tailfriends.user.dto.SettingsResponse;
import com.tailfriends.user.dto.SettingsUpdateRequest;
import com.tailfriends.user.dto.SettingsUpdateRequest.AlertRulesUpdate;
import com.tailfriends.user.repository.UserRepository;
import com.tailfriends.user.repository.UserSettingsRepository;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private ConsentService consentService;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private CurrentUser currentUser;

    @InjectMocks
    private UserSettingsService userSettingsService;

    @BeforeEach
    void 현재_사용자() {
        given(currentUser.id()).willReturn(1L);
    }

    private final User user = User.builder().provider(Provider.KAKAO).providerId("1").nickname("망고집사").build();

    @Test
    void 바꾼_적이_없으면_기본값을_저장하지_않고_돌려준다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userSettingsRepository.findById(1L)).willReturn(Optional.empty());
        given(consentService.getConsents()).willReturn(new ConsentResponse(true, true, false, true));

        SettingsResponse response = userSettingsService.getSettings();

        assertThat(response).isEqualTo(new SettingsResponse(
                LocalTime.of(9, 0), TempUnit.C, false, AlertRules.DEFAULT, true));
        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void 처음_바꾸면_기본값으로_만든_뒤_보낸_값만_바꾼다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userSettingsRepository.findById(1L)).willReturn(Optional.empty());
        given(userSettingsRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(consentService.getConsents()).willReturn(new ConsentResponse(true, true, false, true));

        SettingsResponse response = userSettingsService.updateSettings(new SettingsUpdateRequest(
                LocalTime.of(8, 30), TempUnit.F, null, new AlertRulesUpdate(5, null, null, null), null));

        assertThat(response.notifyTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(response.tempUnit()).isEqualTo(TempUnit.F);
        assertThat(response.alertRules()).isEqualTo(new AlertRules(5, 10, 30, 2));
        verify(consentService, never()).changeOptional(any(), any(), anyBoolean());
    }

    @Test
    void 원본_보관과_알림은_동의_항목으로_바꾼다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userSettingsRepository.findById(1L)).willReturn(Optional.of(UserSettings.defaultsFor(user)));
        given(consentService.getConsents()).willReturn(new ConsentResponse(true, true, true, false));

        SettingsResponse response = userSettingsService.updateSettings(new SettingsUpdateRequest(null, null, true, null, false));

        verify(consentService).changeOptional(eq(1L), eq(ConsentType.KEEP_ORIGINAL), eq(true));
        verify(consentService).changeOptional(eq(1L), eq(ConsentType.NOTIFICATION), eq(false));
        assertThat(response.keepOriginalDefault()).isTrue();
        assertThat(response.pushEnabled()).isFalse();
    }

    @Test
    void 사용자가_없으면_UNAUTHORIZED() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userSettingsService.getSettings())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
