package com.tailfriends.notification.service;

import com.tailfriends.global.security.CurrentUser;
import com.tailfriends.notification.domain.PushToken;
import com.tailfriends.notification.dto.PushTokenRequest;
import com.tailfriends.notification.repository.PushTokenRepository;
import com.tailfriends.user.domain.User;
import com.tailfriends.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushTokenService {

    private final CurrentUser currentUser;
    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    /**
     * 같은 토큰은 한 번만 저장한다. 다른 사용자의 토큰이면(같은 기기에서 계정을 바꿈) 지금 사용자에게 옮긴다.
     */
    @Transactional
    public void register(PushTokenRequest request) {
        User user = userRepository.getReferenceById(currentUser.id());
        pushTokenRepository.findByToken(request.token()).ifPresentOrElse(
                existing -> existing.moveTo(user, request.platform()),
                () -> pushTokenRepository.save(PushToken.of(user, request.token(), request.platform())));
    }

    /**
     * 로그아웃한 기기의 토큰을 지운다. 내 토큰이 아니거나 없으면 아무것도 하지 않는다.
     */
    @Transactional
    public void unregister(String token) {
        pushTokenRepository.deleteByUserIdAndToken(currentUser.id(), token);
    }
}
