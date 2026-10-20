package com.tailfriends.auth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient oauthRestClient;
    private final OAuthProperties properties;

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    public OAuthUserInfo fetchUser(String code, String redirectUri) {
        OAuthProperties.Registration registration = properties.kakao();
        if (registration == null || !StringUtils.hasText(registration.clientId())) {
            throw new IllegalStateException("카카오 로그인 키(KAKAO_CLIENT_ID)가 설정되지 않았어요");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", registration.clientId());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (StringUtils.hasText(registration.clientSecret())) {
            form.add("client_secret", registration.clientSecret());
        }

        try {
            TokenResponse token = oauthRestClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.accessToken() == null) {
                throw new BusinessException(ErrorCode.OAUTH_FAILED);
            }
            UserResponse user = oauthRestClient.get()
                    .uri(USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
                    .retrieve()
                    .body(UserResponse.class);
            if (user == null || user.id() == null) {
                throw new BusinessException(ErrorCode.OAUTH_FAILED);
            }
            return user.toUserInfo();
        } catch (HttpClientErrorException e) {
            // 인가 코드 만료·재사용, redirect_uri 불일치 등은 4xx로 온다
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserResponse(Long id, @JsonProperty("kakao_account") Account account) {

        OAuthUserInfo toUserInfo() {
            String nickname = account == null || account.profile() == null ? null : account.profile().nickname();
            String email = account == null ? null : account.email();
            return new OAuthUserInfo(String.valueOf(id), nickname, email);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Account(String email, Profile profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(String nickname) {
    }
}
