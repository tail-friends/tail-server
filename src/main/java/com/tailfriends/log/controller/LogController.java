package com.tailfriends.log.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.dto.LogCreateRequest;
import com.tailfriends.log.dto.LogCreateResponse;
import com.tailfriends.log.dto.LogListResponse;
import com.tailfriends.log.dto.LogResponse;
import com.tailfriends.log.dto.LogUpdateRequest;
import com.tailfriends.log.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Log")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logs")
public class LogController {

    private final LogService logService;

    @Operation(summary = "기록 작성",
            description = "종류에 해당하는 필드만 저장한다. 기록일은 오늘(KST) 이후 불가. "
                    + "같은 clientId로 이미 저장했으면 409 DUPLICATE_CLIENT_ID와 data.id(기존 기록)를 돌려준다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LogCreateResponse> create(@Valid @RequestBody LogCreateRequest request) {
        return ApiResponse.success(logService.create(request));
    }

    @Operation(summary = "기록 목록", description = "기록일 최신순. cursor는 이전 응답의 nextCursor.")
    @GetMapping
    public ApiResponse<LogListResponse> getLogs(@RequestParam Long animalId,
                                                @RequestParam(required = false) LogType type,
                                                @RequestParam(required = false) Long cursor,
                                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(logService.getLogs(animalId, type, cursor, size));
    }

    @Operation(summary = "기록 수정", description = "보낸 필드만 바꾼다. type은 바꿀 수 없다.")
    @PatchMapping("/{logId}")
    public ApiResponse<LogResponse> update(@PathVariable Long logId,
                                           @Valid @RequestBody LogUpdateRequest request) {
        return ApiResponse.success(logService.update(logId, request));
    }

    @Operation(summary = "기록 삭제", description = "본문 없이 204.")
    @DeleteMapping("/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long logId) {
        logService.delete(logId);
    }
}
