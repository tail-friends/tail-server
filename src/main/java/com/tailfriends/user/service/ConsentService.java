package com.tailfriends.user.service;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
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

    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;

    public ConsentResponse getConsents(Long userId) {
        findUser(userId);
        return ConsentResponse.from(currentOf(userId));
    }

    /**
     * 필수 항목은 true만 받는다. 현재 상태와 다른 항목만 새 행으로 남겨 이력을 쌓는다.
     */
    @Transactional
    public ConsentResponse saveConsents(Long userId, ConsentRequest request) {
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
