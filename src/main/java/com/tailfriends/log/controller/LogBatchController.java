package com.tailfriends.log.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.log.dto.LogBatchRequest;
import com.tailfriends.log.dto.LogBatchResponse;
import com.tailfriends.log.service.LogBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Log")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logs/batch")
public class LogBatchController {

    private final LogBatchService logBatchService;

    @Operation(summary = "일괄 기록",
            description = "여러 개체의 같은 종류 기록(FEED·CLEAN·WEIGHT, 최대 30개)을 한 번에 저장한다. "
                    + "하나라도 실패하면 전부 저장하지 않는다. undoUntil까지 되돌릴 수 있다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LogBatchResponse> create(@Valid @RequestBody LogBatchRequest request) {
        return ApiResponse.success(logBatchService.create(request));
    }

    @Operation(summary = "일괄 기록 되돌리기",
            description = "저장 후 5초(undoUntil) 안에만 통째로 지운다. 지났으면 409 UNDO_EXPIRED. 본문 없이 204.")
    @DeleteMapping("/{batchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void undo(@PathVariable UUID batchId) {
        logBatchService.undo(batchId);
    }
}
