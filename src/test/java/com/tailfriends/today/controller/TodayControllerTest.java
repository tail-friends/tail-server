package com.tailfriends.today.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.today.dto.TodayResponse;
import com.tailfriends.today.dto.TodayResponse.Suggested;
import com.tailfriends.today.dto.TodayResponse.TodayAlert;
import com.tailfriends.today.dto.TodayResponse.TodayDone;
import com.tailfriends.today.dto.TodayResponse.TodayFeed;
import com.tailfriends.today.dto.TodayResponse.TodaySchedule;
import com.tailfriends.today.service.TodayService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TodayController.class)
@WebMvcTestSupport
class TodayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodayService todayService;

    @Test
    @WithMockUserId
    void 오늘_할_일을_명세_형식으로_돌려준다() throws Exception {
        given(todayService.getToday(null)).willReturn(new TodayResponse(LocalDate.of(2026, 10, 18),
                List.of(new TodayFeed(12L, "망고", "https://files/thumb.jpg", FeedStatus.DUE, null,
                                new Suggested("귀뚜라미", 6)),
                        new TodayFeed(15L, "초코", null, FeedStatus.OVERDUE, 2, null)),
                List.of(new TodaySchedule(41L, 12L, "칼슘 더스팅", LocalDate.of(2026, 10, 16))),
                List.of(new TodayAlert(77L, 15L, AlertType.REFUSAL_ALERT)),
                List.of(new TodayDone("CLEAN", 12L, 899L, null), new TodayDone("SCHEDULE", null, null, 42L))));

        mockMvc.perform(get("/api/v1/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-10-18"))
                .andExpect(jsonPath("$.data.feeds[0].status").value("DUE"))
                .andExpect(jsonPath("$.data.feeds[0].overdueDays").doesNotExist())
                .andExpect(jsonPath("$.data.feeds[0].suggested.food").value("귀뚜라미"))
                .andExpect(jsonPath("$.data.feeds[1].overdueDays").value(2))
                .andExpect(jsonPath("$.data.feeds[1].thumbnailUrl").isEmpty())
                .andExpect(jsonPath("$.data.schedules[0].nextDate").value("2026-10-16"))
                .andExpect(jsonPath("$.data.alerts[0].type").value("REFUSAL_ALERT"))
                .andExpect(jsonPath("$.data.done[0].logId").value(899))
                .andExpect(jsonPath("$.data.done[0].scheduleId").doesNotExist())
                .andExpect(jsonPath("$.data.done[1].type").value("SCHEDULE"))
                .andExpect(jsonPath("$.data.done[1].animalId").isEmpty())
                .andExpect(jsonPath("$.data.done[1].scheduleId").value(42))
                .andExpect(jsonPath("$.data.done[1].logId").doesNotExist());
    }

    @Test
    @WithMockUserId
    void 날짜를_넘기면_서비스에_전달하고_오늘이_아니면_400() throws Exception {
        given(todayService.getToday(LocalDate.of(2026, 10, 17)))
                .willThrow(new BusinessException(ErrorCode.INVALID_INPUT, "오늘 날짜만 조회할 수 있어요"));

        mockMvc.perform(get("/api/v1/today").param("date", "2026-10-17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        verify(todayService).getToday(LocalDate.of(2026, 10, 17));
    }

    @Test
    @WithMockUserId
    void 날짜_형식이_틀리면_400() throws Exception {
        mockMvc.perform(get("/api/v1/today").param("date", "2026/10/17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/today"))
                .andExpect(status().isUnauthorized());
    }
}
