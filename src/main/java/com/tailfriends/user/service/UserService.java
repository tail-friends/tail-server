package com.tailfriends.user.service;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.dto.MeResponse;
import com.tailfriends.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;

    /**
     * 토큰은 유효하지만 사용자가 없으면(탈퇴 등) 다시 로그인하도록 UNAUTHORIZED.
     */
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return MeResponse.of(user, animalRepository.countByUserId(userId));
    }
}
