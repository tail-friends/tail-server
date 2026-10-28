package com.tailfriends.point.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.point.domain.PointReason;
import com.tailfriends.point.dto.PointHistoryListResponse;
import com.tailfriends.point.dto.PointHistoryResponse;
import com.tailfriends.point.service.PointService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointController.class)
@WebMvcTestSupport
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    @Test
    @WithMockUserId(7L)
    void 잔액과_내역과_다음_커서를_돌려준다() throws Exception {
        given(pointService.getHistory(null, 20)).willReturn(new PointHistoryListResponse(120, List.of(
                new PointHistoryResponse(501L, 10, PointReason.LOG_FEED, 12L, 902L, Instant.parse("2026-10-18T09:00:00Z"))),
                501L));

        mockMvc.perform(get("/api/v1/points/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(120))
                .andExpect(jsonPath("$.data.history[0].amount").value(10))
                .andExpect(jsonPath("$.data.history[0].reason").value("LOG_FEED"))
                .andExpect(jsonPath("$.data.history[0].animalId").value(12))
                .andExpect(jsonPath("$.data.history[0].logId").value(902))
                .andExpect(jsonPath("$.data.history[0].createdAt").value("2026-10-18T09:00:00Z"))
                .andExpect(jsonPath("$.data.nextCursor").value(501));
    }

    @Test
    @WithMockUserId(7L)
    void size가_1에서_100을_벗어나면_400() throws Exception {
        mockMvc.perform(get("/api/v1/points/history").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/points/history"))
                .andExpect(status().isUnauthorized());
    }
}
