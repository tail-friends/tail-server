package com.tailfriends.user.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.user.domain.Consent;
import com.tailfriends.user.domain.ConsentType;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.dto.ConsentRequest;
import com.tailfriends.user.dto.ConsentResponse;
import com.tailfriends.user.repository.ConsentRepository;
import com.tailfriends.user.repository.UserRepository;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;

    public ConsentResponse getConsents() {
        Long userId = currentUser.id();
        findUser(userId);
        return ConsentResponse.from(currentOf(userId));
    }

    /**
     * 항목에 지금 동의한 상태인지. 답한 적 없으면 false.
     */
    public boolean isAgreed(Long userId, ConsentType type) {
        return Boolean.TRUE.equals(currentOf(userId).get(type));
    }

    /**
     * 필수 항목은 true만 받는다. 현재 상태와 다른 항목만 새 행으로 남겨 이력을 쌓는다.
     */
    @Transactional
    public ConsentResponse saveConsents(ConsentRequest request) {
        Long userId = currentUser.id();
        User user = findUser(userId);
        Map<ConsentType, Boolean> requested = request.toMap();
        requested.forEach((type, agreed) -> {
            if (type.isRequired() && !agreed) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "필수 항목에 동의해야 해요");
            }
        });

        Map<ConsentType, Boolean> current = currentOf(userId);
        requested.forEach((type, agreed) -> {
            if (!Objects.equals(current.get(type), agreed)) {
                consentRepository.save(Consent.of(user, type, agreed));
            }
        });
        return ConsentResponse.from(requested);
    }

    /**
     * 선택 항목 하나만 바꾼다. 설정 화면의 원본 사진 보관·알림 켜기/끄기용. 현재 상태와 같으면 남기지 않는다.
     */
    @Transactional
    public void changeOptional(Long userId, ConsentType type, boolean agreed) {
        if (type.isRequired()) {
            throw new IllegalArgumentException("필수 항목은 설정에서 바꿀 수 없어요: " + type);
        }
        User user = findUser(userId);
        if (!Objects.equals(currentOf(userId).get(type), agreed)) {
            consentRepository.save(Consent.of(user, type, agreed));
        }
    }

    /**
     * 항목별 가장 최근 행. 답한 적 없는 항목은 맵에 없다.
     */
    private Map<ConsentType, Boolean> currentOf(Long userId) {
        Map<ConsentType, Boolean> current = new EnumMap<>(ConsentType.class);
        for (Consent consent : consentRepository.findAllByUserIdOrderByIdDesc(userId)) {
            current.putIfAbsent(consent.getType(), consent.isAgreed());
        }
        return current;
    }

    /**
     * 토큰은 유효하지만 사용자가 없으면(탈퇴 등) 다시 로그인하도록 UNAUTHORIZED.
     */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
