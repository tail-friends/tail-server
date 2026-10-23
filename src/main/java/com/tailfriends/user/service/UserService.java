package com.tailfriends.user.service;

import com.tailfriends.animal.repository.AnimalRepository;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.photo.service.PhotoFileRemover;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.dto.MeResponse;
import com.tailfriends.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final PhotoFileRemover photoFileRemover;

    /**
     * 토큰은 유효하지만 사용자가 없으면(탈퇴 등) 다시 로그인하도록 UNAUTHORIZED.
     */
    @Transactional(readOnly = true)
    public MeResponse getMe() {
        Long userId = currentUser.id();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return MeResponse.of(user, animalRepository.countByUserId(userId));
    }

    /**
     * 계정을 즉시 지운다. 개체·기록·건강 경고·캐릭터·사진·리프레시 토큰은 DB의 ON DELETE CASCADE로 함께 지워진다.
     * 사진 파일은 커밋 후 지운다.
     * 이미 없는 사용자면 getMe와 같이 UNAUTHORIZED.
     */
    @Transactional
    public void withdraw() {
        Long userId = currentUser.id();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        photoFileRemover.removeFilesOfUserAfterCommit(userId);
        userRepository.delete(user);
    }
}
