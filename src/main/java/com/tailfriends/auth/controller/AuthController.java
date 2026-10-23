package com.tailfriends.auth.controller;

import com.tailfriends.auth.dto.LogoutRequest;
import com.tailfriends.auth.dto.OAuthLoginRequest;
import com.tailfriends.auth.dto.OAuthLoginResponse;
import com.tailfriends.auth.dto.TokenReissueRequest;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.service.OAuthService;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OAuthService oauthService;
    private final TokenService tokenService;

    @Operation(summary = "소셜 로그인",
            description = "앱이 받은 인가 코드로 가입 또는 로그인하고 토큰을 발급한다. 처음 로그인하면 isNewUser: true. "
                    + "지금은 KAKAO, GOOGLE만 지원한다.")
    @PostMapping("/oauth/callback")
    public ApiResponse<OAuthLoginResponse> login(@Valid @RequestBody OAuthLoginRequest request) {
        return ApiResponse.success(oauthService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 다시 발급한다. 리프레시 토큰도 함께 교체한다.")
    @PutMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        return ApiResponse.success(tokenService.reissue(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기한다. 본문 없이 204.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        tokenService.revoke(request.refreshToken());
    }
}
