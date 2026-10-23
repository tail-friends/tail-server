package com.tailfriends.auth.controller;

import com.tailfriends.auth.dto.SampleDataResponse;
import com.tailfriends.auth.service.DevSampleDataService;
import com.tailfriends.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬에서 앱·웹 화면을 채울 샘플 데이터를 만든다. dev.login.enabled=true일 때만 생긴다.
 */
@Tag(name = "Dev")
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dev.login.enabled", havingValue = "true")
public class DevSampleDataController {

    private final DevSampleDataService devSampleDataService;

    @Operation(summary = "샘플 데이터 만들기 (로컬 전용)",
            description = "로그인한 사용자에게 개체 3마리(급여 여유 / 급여일 지남 / 오늘 급여·탈피 준비)와 캐릭터, "
                    + "최근 60일 기록을 만든다. 부를 때마다 새로 추가된다. 운영에는 없다.")
    @PostMapping("/api/v1/dev/sample-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SampleDataResponse> create() {
        return ApiResponse.success(devSampleDataService.create());
    }
}
