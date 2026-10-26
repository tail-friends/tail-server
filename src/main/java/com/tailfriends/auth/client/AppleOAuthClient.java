package com.tailfriends.auth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.user.domain.Provider;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * iOS 앱의 애플 로그인. 앱이 받은 인가 코드를 애플 토큰 API로 바꾸고, 함께 온 ID 토큰에서 사용자를 읽는다.
 * 애플은 이름을 ID 토큰에 주지 않아 nickname은 항상 null이다.
 */
@Component
@RequiredArgsConstructor
public class AppleOAuthClient implements OAuthClient {

    static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
    static final String ISSUER = "https://appleid.apple.com";
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);

    private final RestClient oauthRestClient;
    private final OAuthProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Override
    public Provider provider() {
        return Provider.APPLE;
    }

    /**
     * iOS 앱에서 받은 인가 코드는 redirect_uri 없이 교환하므로 redirectUri는 쓰지 않는다.
     */
    @Override
    public OAuthUserInfo fetchUser(String code, String redirectUri) {
        OAuthProperties.Apple apple = properties.apple();
        if (apple == null || !StringUtils.hasText(apple.clientId()) || !StringUtils.hasText(apple.teamId())
                || !StringUtils.hasText(apple.keyId()) || !StringUtils.hasText(apple.privateKey())) {
            throw new IllegalStateException(
                    "애플 로그인 키(APPLE_CLIENT_ID, APPLE_TEAM_ID, APPLE_KEY_ID, APPLE_PRIVATE_KEY)가 설정되지 않았어요");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", apple.clientId());
        form.add("client_secret", clientSecret(apple));
        form.add("code", code);

        try {
            TokenResponse token = oauthRestClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.idToken() == null) {
                throw new BusinessException(ErrorCode.OAUTH_FAILED);
            }
            IdTokenClaims claims = readClaims(token.idToken());
            validate(claims, apple.clientId());
            return new OAuthUserInfo(claims.sub(), null, claims.email());
        } catch (HttpClientErrorException e) {
            // 인가 코드 만료·재사용, client secret 서명 오류 등은 4xx(invalid_grant, invalid_client)로 온다
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    /**
     * 애플이 요구하는 client secret. 팀 ID가 발급하고 앱 번들 ID가 주체인 ES256 JWT다.
     */
    String clientSecret(OAuthProperties.Apple apple) {
        Instant now = clock.instant();
        return Jwts.builder()
                .header().keyId(apple.keyId()).and()
                .issuer(apple.teamId())
                .subject(apple.clientId())
                .audience().add(ISSUER).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(CLIENT_SECRET_TTL)))
                .signWith(privateKeyOf(apple.privateKey()), Jwts.SIG.ES256)
                .compact();
    }

    /**
     * ID 토큰은 client secret으로 인증한 요청의 응답으로 애플에게서 직접(TLS) 받으므로 서명은 확인하지 않는다.
     * (OpenID Connect Core 3.1.3.7) 대신 발급자, 대상, 만료는 확인한다.
     */
    private IdTokenClaims readClaims(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
        try {
            return objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), IdTokenClaims.class);
        } catch (IllegalArgumentException | IOException e) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    private void validate(IdTokenClaims claims, String clientId) {
        boolean valid = ISSUER.equals(claims.iss())
                && clientId.equals(claims.aud())
                && claims.exp() != null && Instant.ofEpochSecond(claims.exp()).isAfter(clock.instant())
                && StringUtils.hasText(claims.sub());
        if (!valid) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    private static PrivateKey privateKeyOf(String pem) {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException("애플 로그인 서명 키(APPLE_PRIVATE_KEY)를 읽지 못했어요", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("id_token") String idToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IdTokenClaims(String iss, String aud, Long exp, String sub, String email) {
    }
}
