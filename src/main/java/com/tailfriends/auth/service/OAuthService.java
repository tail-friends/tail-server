package com.tailfriends.auth.service;

import com.tailfriends.auth.client.OAuthClient;
import com.tailfriends.auth.client.OAuthUserInfo;
import com.tailfriends.auth.dto.OAuthLoginRequest;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 소셜 로그인. 소셜 서버 호출은 DB 트랜잭션 밖에서 하고, 가입·토큰 발급만 SignInService에서 한 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final List<OAuthClient> oauthClients;
    private final SignInService signInService;

    public OAuthLoginResponse login(OAuthLoginRequest request) {
        OAuthClient client = oauthClients.stream()
                .filter(candidate -> candidate.provider() == request.provider())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "아직 지원하지 않는 로그인 방식이에요"));
        OAuthUserInfo userInfo = client.fetchUser(request.code(), request.redirectUri());
        return signInService.signIn(request.provider(), userInfo);
    }
}
