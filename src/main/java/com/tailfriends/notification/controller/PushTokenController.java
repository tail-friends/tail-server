package com.tailfriends.notification.controller;

import com.tailfriends.notification.dto.PushTokenRequest;
import com.tailfriends.notification.service.PushTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @Operation(summary = "푸시 토큰 등록",
            description = "기기의 FCM 토큰을 등록한다. 같은 토큰은 한 번만 저장하고, 다른 계정에서 등록했던 토큰이면 "
                    + "지금 계정으로 옮긴다. 본문 없이 204.")
    @PostMapping("/api/v1/users/me/push-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody PushTokenRequest request) {
        pushTokenService.register(request);
    }
}
