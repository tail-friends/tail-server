package com.tailfriends.user.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.domain.AlertRules;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.domain.UserSettings;
import com.tailfriends.user.dto.ConsentResponse;
import com.tailfriends.user.dto.SettingsResponse;
import com.tailfriends.user.dto.SettingsUpdateRequest;
import com.tailfriends.user.repository.UserRepository;
import com.tailfriends.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 설정. 원본 사진 보관·알림 수신은 동의(consents)에 있어 ConsentService로 읽고 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final ConsentService consentService;

    /**
     * 설정을 바꾼 적이 없으면 기본값. 저장하지 않는다.
     */
    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        Long userId = currentUser.id();
        User user = findUser(userId);
        UserSettings settings = userSettingsRepository.findById(userId)
                .orElseGet(() -> UserSettings.defaultsFor(user));
        return toResponse(userId, settings);
    }

    /**
     * 건강 경고 판정 기준. 설정을 바꾼 적이 없으면 기본값.
     */
    public AlertRules alertRulesOf(Long userId) {
        return userSettingsRepository.findById(userId)
                .map(UserSettings::alertRules)
                .orElse(AlertRules.DEFAULT);
    }

    @Transactional
    public SettingsResponse updateSettings(SettingsUpdateRequest request) {
        Long userId = currentUser.id();
        User user = findUser(userId);
        UserSettings settings = userSettingsRepository.findById(userId)
                .orElseGet(() -> userSettingsRepository.save(UserSettings.defaultsFor(user)));

        SettingsUpdateRequest.AlertRulesUpdate rules = request.alertRules();
        settings.update(
                request.notifyTime(),
                request.tempUnit(),
                rules == null ? null : rules.refusalCount(),
                rules == null ? null : rules.weightLossPercent(),
                rules == null ? null : rules.weightLossDays(),
                rules == null ? null : rules.looseStoolCount());
        if (request.keepOriginalDefault() != null) {
            consentService.changeOptional(userId, ConsentType.KEEP_ORIGINAL, request.keepOriginalDefault());
        }
        if (request.pushEnabled() != null) {
            consentService.changeOptional(userId, ConsentType.NOTIFICATION, request.pushEnabled());
        }
        return toResponse(userId, settings);
    }

    private SettingsResponse toResponse(Long userId, UserSettings settings) {
        ConsentResponse consents = consentService.getConsents();
        return new SettingsResponse(
                settings.getNotifyTime(),
                settings.getTempUnit(),
                consents.keepOriginal(),
                settings.alertRules(),
                consents.notification());
    }

    /**
     * 토큰은 유효하지만 사용자가 없으면(탈퇴 등) 다시 로그인하도록 UNAUTHORIZED.
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
