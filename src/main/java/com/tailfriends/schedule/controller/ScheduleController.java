package com.tailfriends.schedule.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.schedule.dto.ScheduleCompleteRequest;
import com.tailfriends.schedule.dto.ScheduleCompleteResponse;
import com.tailfriends.schedule.dto.ScheduleCreateRequest;
import com.tailfriends.schedule.dto.ScheduleCreateResponse;
import com.tailfriends.schedule.dto.ScheduleListResponse;
import com.tailfriends.schedule.dto.ScheduleResponse;
import com.tailfriends.schedule.dto.ScheduleUpdateRequest;
import com.tailfriends.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Schedule")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "반복 일정 목록",
            description = "다음 일정일 순, 끝난 일정(nextDate null)은 맨 뒤. animalId가 있으면 그 개체의 일정과 공통 일정.")
    @GetMapping
    public ApiResponse<ScheduleListResponse> getSchedules(@RequestParam(required = false) Long animalId) {
        return ApiResponse.success(scheduleService.getSchedules(animalId));
    }

    @Operation(summary = "반복 일정 생성",
            description = "startDate가 첫 일정일이다. animalId가 없으면 모든 개체 공통 일정.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleCreateResponse> create(@Valid @RequestBody ScheduleCreateRequest request) {
        return ApiResponse.success(scheduleService.create(request));
    }

    @Operation(summary = "반복 일정 수정",
            description = "보낸 필드만 바꾼다. 반복·시작일·종료일이 바뀌면 다음 일정일을 다시 계산한다.")
    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> update(@PathVariable Long scheduleId,
                                                @Valid @RequestBody ScheduleUpdateRequest request) {
        return ApiResponse.success(scheduleService.update(scheduleId, request));
    }

    @Operation(summary = "반복 일정 삭제", description = "본문 없이 204.")
    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long scheduleId) {
        scheduleService.delete(scheduleId);
    }

    @Operation(summary = "일정 완료 처리",
            description = "완료한 날 기준으로 다음 일정일을 정한다. 완료일은 오늘(KST) 이후 불가.")
    @PostMapping("/{scheduleId}/complete")
    public ApiResponse<ScheduleCompleteResponse> complete(@PathVariable Long scheduleId,
                                                          @Valid @RequestBody ScheduleCompleteRequest request) {
        return ApiResponse.success(scheduleService.complete(scheduleId, request));
    }
}
