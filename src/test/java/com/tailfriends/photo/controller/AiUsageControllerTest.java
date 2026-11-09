package com.tailfriends.photo.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.photo.dto.AiUsageResponse;
import com.tailfriends.photo.service.AiUsageService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiUsageController.class)
@WebMvcTestSupport
class AiUsageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiUsageService aiUsageService;

    @Test
    @WithMockUserId(7L)
    void 오늘_AI_분석_횟수를_명세_형식으로_돌려준다() throws Exception {
        given(aiUsageService.getTodayUsage()).willReturn(
                new AiUsageResponse(5, 3, Instant.parse("2026-10-18T15:00:00Z")));

        mockMvc.perform(get("/api/v1/photos/ai-usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.limit").value(5))
                .andExpect(jsonPath("$.data.used").value(3))
                .andExpect(jsonPath("$.data.resetsAt").value("2026-10-18T15:00:00Z"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/photos/ai-usage"))
                .andExpect(status().isUnauthorized());
    }
}
