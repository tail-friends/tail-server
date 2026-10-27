package com.tailfriends.today.controller;

import com.tailfriends.global.response.ApiResponse;
import com.tailfriends.today.dto.TodayResponse;
import com.tailfriends.today.service.TodayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Today")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/today")
public class TodayController {

    private final TodayService todayService;

    @Operation(summary = "오늘 할 일",
            description = "급여가 오늘이거나 지난 개체, 오늘이거나 지난 일정, 열린 건강 경고, 오늘 완료한 기록·일정. "
                    + "date를 생략하면 오늘(KST)이고, 지금은 오늘만 된다.")
    @GetMapping
    public ApiResponse<TodayResponse> getToday(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return ApiResponse.success(todayService.getToday(date));
    }
}
