package com.tailfriends.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.Consent;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.domain.Provider;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.dto.ConsentRequest;
import com.tailfriends.user.dto.ConsentResponse;
import com.tailfriends.user.repository.ConsentRepository;
import com.tailfriends.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConsentRepository consentRepository;

    @InjectMocks
    private ConsentService consentService;

    private final User user = User.builder().provider(Provider.KAKAO).providerId("1").nickname("망고집사").build();

    @Test
    void 항목별_가장_최근_행이_현재_상태이고_답하지_않은_항목은_false() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(consentRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of(
                Consent.of(user, ConsentType.NOTIFICATION, false),
                Consent.of(user, ConsentType.PRIVACY, true),
                Consent.of(user, ConsentType.NOTIFICATION, true)));

        ConsentResponse response = consentService.getConsents(1L);

        assertThat(response).isEqualTo(new ConsentResponse(true, false, false, false));
    }

    @Test
    void 처음_저장하면_네_항목을_모두_남긴다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(consentRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of());

        ConsentResponse response = consentService.saveConsents(1L, new ConsentRequest(true, true, false, true));

        verify(consentRepository, times(4)).save(any());
        assertThat(response).isEqualTo(new ConsentResponse(true, true, false, true));
    }

    @Test
    void 바뀐_항목만_새_행으로_남긴다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(consentRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of(
                Consent.of(user, ConsentType.PRIVACY, true),
                Consent.of(user, ConsentType.PHOTO_ANALYSIS, true),
                Consent.of(user, ConsentType.KEEP_ORIGINAL, false),
                Consent.of(user, ConsentType.NOTIFICATION, true)));

        consentService.saveConsents(1L, new ConsentRequest(true, true, true, false));

        ArgumentCaptor<Consent> saved = ArgumentCaptor.forClass(Consent.class);
        verify(consentRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Consent::getType, Consent::isAgreed)
                .containsExactly(
                        tuple(ConsentType.KEEP_ORIGINAL, true),
                        tuple(ConsentType.NOTIFICATION, false));
    }

    @Test
    void 필수_항목이_false면_INVALID_INPUT() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> consentService.saveConsents(1L, new ConsentRequest(true, false, true, true)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verify(consentRepository, never()).save(any());
    }

    @Test
    void 사용자가_없으면_UNAUTHORIZED() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> consentService.getConsents(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 선택_항목_하나를_바꾸면_새_행으로_남긴다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(consentRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of(
                Consent.of(user, ConsentType.NOTIFICATION, true)));

        consentService.changeOptional(1L, ConsentType.NOTIFICATION, false);

        verify(consentRepository).save(argThat(consent ->
                consent.getType() == ConsentType.NOTIFICATION && !consent.isAgreed()));
    }

    @Test
    void 선택_항목이_이미_같은_값이면_남기지_않는다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(consentRepository.findAllByUserIdOrderByIdDesc(1L)).willReturn(List.of(
                Consent.of(user, ConsentType.KEEP_ORIGINAL, true)));

        consentService.changeOptional(1L, ConsentType.KEEP_ORIGINAL, true);

        verify(consentRepository, never()).save(any());
    }

    @Test
    void 필수_항목은_하나만_바꿀_수_없다() {
        assertThatThrownBy(() -> consentService.changeOptional(1L, ConsentType.PRIVACY, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
