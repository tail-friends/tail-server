package com.tailfriends.log.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.log.dto.LogSyncRequest;
import com.tailfriends.log.dto.LogSyncResponse;
import com.tailfriends.log.service.LogSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Log")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logs/sync")
public class LogSyncController {

    private final LogSyncService logSyncService;

    @Operation(summary = "오프라인 기록 동기화",
            description = "오프라인에 쌓인 기록(최대 200건, clientId 필수)을 항목마다 따로 저장한다. "
                    + "status는 CREATED·DUPLICATE(이미 있는 clientId, id는 기존 기록)·FAILED(error 포함).")
    @PostMapping
    public ApiResponse<LogSyncResponse> sync(@Valid @RequestBody LogSyncRequest request) {
        return ApiResponse.success(logSyncService.sync(request));
    }
}
