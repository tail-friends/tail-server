package com.tailfriends.auth.controller;

import com.tailfriends.auth.dto.LogoutRequest;
import com.tailfriends.auth.dto.TokenReissueRequest;
import com.tailfriends.auth.dto.TokenResponse;
import com.tailfriends.auth.service.TokenService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final TokenService tokenService;

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 다시 발급한다. 리프레시 토큰도 함께 교체한다.")
    @PutMapping("/reissue")
    public ApiResponse<TokenResponse> reissue(@Valid @RequestBody TokenReissueRequest request) {
        return ApiResponse.success(tokenService.reissue(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기한다. 본문 없이 204.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Long userId, @Valid @RequestBody LogoutRequest request) {
        tokenService.revoke(userId, request.refreshToken());
    }
}
