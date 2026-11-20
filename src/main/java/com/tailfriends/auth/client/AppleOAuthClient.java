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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 애플 로그인. 받은 인가 코드를 애플 토큰 API로 바꾸고, 함께 온 ID 토큰에서 사용자를 읽는다.
 * 애플은 이름을 ID 토큰에 주지 않아 nickname은 항상 null이다.
 * iOS 앱은 번들 ID로, 관리자 웹은 Services ID로 교환한다. 두 ID가 같은 App ID 그룹이면 애플 사용자 ID(sub)가 같다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleOAuthClient implements OAuthClient {

    static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
    static final String REVOKE_URL = "https://appleid.apple.com/auth/revoke";
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
     * 탈퇴할 때 철회하도록 refresh token도 돌려준다.
     */
    @Override
    public OAuthUserInfo fetchUser(String code, String redirectUri) {
        OAuthProperties.Apple apple = requireApple();
        return exchange(apple, apple.clientId(), code, null);
    }

    /**
     * 관리자 웹이 애플 JS SDK로 받은 인가 코드. Services ID와 인가 요청 때의 redirect_uri로 교환한다.
     * 탈퇴 철회는 앱 키로 하므로 웹에서 받은 refresh token은 돌려주지 않는다(저장된 앱 토큰을 덮어쓰지 않게).
     *
     * @throws BusinessException Services ID(APPLE_WEB_CLIENT_ID)가 설정되지 않았으면 INVALID_INPUT
     */
    @Override
    public OAuthUserInfo fetchUserForWeb(String code, String redirectUri) {
        OAuthProperties.Apple apple = requireApple();
        if (!StringUtils.hasText(apple.webClientId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "웹에서는 아직 애플로 로그인할 수 없어요");
        }
        OAuthUserInfo user = exchange(apple, apple.webClientId(), code, redirectUri);
        return new OAuthUserInfo(user.providerId(), user.nickname(), user.email());
    }

    /**
     * @param redirectUri 웹이면 인가 요청 때의 값, 앱이면 null(보내지 않음)
     */
    private OAuthUserInfo exchange(OAuthProperties.Apple apple, String clientId, String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret(apple, clientId));
        form.add("code", code);
        if (redirectUri != null) {
            form.add("redirect_uri", redirectUri);
        }

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
            validate(claims, clientId);
            return new OAuthUserInfo(claims.sub(), null, claims.email(), token.refreshToken());
        } catch (HttpClientErrorException e) {
            // 인가 코드 만료·재사용, client secret 서명 오류 등은 4xx(invalid_grant, invalid_client)로 온다
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    /**
     * 회원 탈퇴 때 애플 토큰을 철회한다 (App Store 심사 가이드라인 5.1.1(v)).
     *
     * @return 철회했으면 true. 애플이 거절하거나(이미 철회됨 등) 연결하지 못하면 로그를 남기고 false
     */
    public boolean revoke(String refreshToken) {
        OAuthProperties.Apple apple = requireApple();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", apple.clientId());
        form.add("client_secret", clientSecret(apple));
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            oauthRestClient.post()
                    .uri(REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("애플 토큰을 철회하지 못했어요: {}", e.getMessage());
            return false;
        }
    }

    private OAuthProperties.Apple requireApple() {
        OAuthProperties.Apple apple = properties.apple();
        if (apple == null || !StringUtils.hasText(apple.clientId()) || !StringUtils.hasText(apple.teamId())
                || !StringUtils.hasText(apple.keyId()) || !StringUtils.hasText(apple.privateKey())) {
            throw new IllegalStateException(
                    "애플 로그인 키(APPLE_CLIENT_ID, APPLE_TEAM_ID, APPLE_KEY_ID, APPLE_PRIVATE_KEY)가 설정되지 않았어요");
        }
        return apple;
    }

    /**
     * 애플이 요구하는 client secret. 팀 ID가 발급하고 앱 번들 ID가 주체인 ES256 JWT다.
     */
    String clientSecret(OAuthProperties.Apple apple) {
        return clientSecret(apple, apple.clientId());
    }

    /**
     * @param clientId 주체. 앱이면 번들 ID, 웹이면 Services ID
     */
    String clientSecret(OAuthProperties.Apple apple, String clientId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .header().keyId(apple.keyId()).and()
                .issuer(apple.teamId())
                .subject(clientId)
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
    record TokenResponse(@JsonProperty("id_token") String idToken,
                         @JsonProperty("refresh_token") String refreshToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IdTokenClaims(String iss, String aud, Long exp, String sub, String email) {
    }
}
