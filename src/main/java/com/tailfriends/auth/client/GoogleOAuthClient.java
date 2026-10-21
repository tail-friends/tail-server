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
public class GoogleOAuthClient implements OAuthClient {

    static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String USER_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final RestClient oauthRestClient;
    private final OAuthProperties properties;

    @Override
    public Provider provider() {
        return Provider.GOOGLE;
    }

    @Override
    public OAuthUserInfo fetchUser(String code, String redirectUri) {
        OAuthProperties.Registration registration = properties.google();
        if (registration == null || !StringUtils.hasText(registration.clientId())) {
            throw new IllegalStateException("구글 로그인 키(GOOGLE_CLIENT_ID)가 설정되지 않았어요");
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
            if (user == null || user.sub() == null) {
                throw new BusinessException(ErrorCode.OAUTH_FAILED);
            }
            return new OAuthUserInfo(user.sub(), user.name(), user.email());
        } catch (HttpClientErrorException e) {
            // 인가 코드 만료·재사용, redirect_uri 불일치 등은 4xx로 온다
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UserResponse(String sub, String name, String email) {
    }
}
