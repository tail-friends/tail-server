package com.tailfriends.auth.controller;

import com.tailfriends.global.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 웹(관리자 웹)은 리프레시 토큰을 자바스크립트가 읽을 수 없는 httpOnly 쿠키로 주고받는다.
 * 요청 헤더 {@code X-Client-Type: WEB}이면 웹으로 보고, 앱은 지금처럼 응답 본문을 쓴다.
 */
@Component
public class RefreshTokenCookies {

    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    public static final String WEB = "WEB";
    static final String NAME = "refresh_token";
    static final String PATH = "/api/v1/auth";

    private final Duration maxAge;
    private final boolean secure;

    public RefreshTokenCookies(JwtProperties jwtProperties,
                               @Value("${auth.refresh-cookie.secure:true}") boolean secure) {
        this.maxAge = jwtProperties.refreshTokenExpiration();
        this.secure = secure;
    }

    public boolean isWeb(HttpServletRequest request) {
        return WEB.equalsIgnoreCase(request.getHeader(CLIENT_TYPE_HEADER));
    }

    /**
     * 본문에 토큰이 있으면 그것을, 없으면 쿠키의 토큰을 쓴다.
     */
    public Optional<String> resolve(String bodyToken, HttpServletRequest request) {
        if (StringUtils.hasText(bodyToken)) {
            return Optional.of(bodyToken);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void write(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshToken, maxAge).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
