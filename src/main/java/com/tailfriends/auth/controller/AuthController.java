package com.tailfriends.auth.controller;

import com.tailfriends.auth.dto.LogoutRequest;
import com.tailfriends.auth.dto.OAuthLoginRequest;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.TokenReissueRequest;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.service.OAuthService;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.notification.service.PushTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 요청 헤더 X-Client-Type: WEB이면 리프레시 토큰을 httpOnly 쿠키로 주고받는다. ({@link RefreshTokenCookies})
 */
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String WEB_DESCRIPTION = " X-Client-Type: WEB 헤더를 보내면 리프레시 토큰을 응답 본문 대신 "
            + "httpOnly 쿠키(refresh_token, Path=/api/v1/auth)로 내려주고 본문의 refreshToken은 null이다.";

    private final OAuthService oauthService;
    private final TokenService tokenService;
    private final PushTokenService pushTokenService;
    private final RefreshTokenCookies refreshTokenCookies;

    @Operation(summary = "소셜 로그인",
            description = "앱이 받은 인가 코드로 가입 또는 로그인하고 토큰을 발급한다. 처음 로그인하면 isNewUser: true. "
                    + "APPLE은 앱이면 iOS 앱에서 받은 인가 코드로 redirectUri를 쓰지 않고, 웹(X-Client-Type: WEB)이면 "
                    + "애플 JS SDK로 받은 인가 코드와 그때의 redirectUri를 Services ID로 교환한다. "
                    + "애플은 이름을 주지 않아 기본 닉네임으로 가입된다."
                    + WEB_DESCRIPTION)
    @PostMapping("/oauth/callback")
    public ApiResponse<OAuthLoginResponse> login(@Valid @RequestBody OAuthLoginRequest request,
                                                 HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        boolean web = refreshTokenCookies.isWeb(httpRequest);
        OAuthLoginResponse response = oauthService.login(request, web);
        if (!web) {
            return ApiResponse.success(response);
        }
        refreshTokenCookies.write(httpResponse, response.refreshToken());
        return ApiResponse.success(response.withoutRefreshToken());
    }

    @Operation(summary = "토큰 재발급",
            description = "리프레시 토큰으로 액세스 토큰을 다시 발급한다. 리프레시 토큰도 함께 교체한다. "
                    + "본문에 refreshToken이 없으면 쿠키의 리프레시 토큰을 쓰고, 둘 다 없으면 INVALID_REFRESH_TOKEN(401)."
                    + WEB_DESCRIPTION)
    @PutMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@RequestBody(required = false) TokenReissueRequest request,
                                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String refreshToken = refreshTokenCookies
                .resolve(request == null ? null : request.refreshToken(), httpRequest)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        TokenResponse response = tokenService.reissue(refreshToken);
        if (!refreshTokenCookies.isWeb(httpRequest)) {
            return ApiResponse.success(response);
        }
        refreshTokenCookies.write(httpResponse, response.refreshToken());
        return ApiResponse.success(response.withoutRefreshToken());
    }

    @Operation(summary = "로그아웃",
            description = "리프레시 토큰을 폐기한다. pushToken을 보내면 이 기기의 푸시 토큰도 지운다. 본문 없이 204. "
                    + "본문에 refreshToken이 없으면 쿠키의 리프레시 토큰을 폐기하고, 쿠키가 있었으면 쿠키도 지운다. "
                    + "액세스 토큰이 필요하다. 만료됐으면 먼저 재발급한다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest request,
                       HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String bodyToken = request == null ? null : request.refreshToken();
        refreshTokenCookies.resolve(bodyToken, httpRequest).ifPresent(tokenService::revoke);
        if (request != null && StringUtils.hasText(request.pushToken())) {
            pushTokenService.unregister(request.pushToken());
        }
        if (refreshTokenCookies.isWeb(httpRequest) || !StringUtils.hasText(bodyToken)) {
            refreshTokenCookies.clear(httpResponse);
        }
    }
}
