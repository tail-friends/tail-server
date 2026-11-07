package com.tailfriends.health.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.health.domain.HealthCheckItem;
import com.tailfriends.health.domain.HealthCheckKey;
import com.tailfriends.health.domain.HealthCheckResult;
import com.tailfriends.health.dto.HealthCheckListResponse;
import com.tailfriends.health.dto.HealthCheckResponse;
import com.tailfriends.health.dto.HealthCheckSummaryResponse;
import com.tailfriends.health.service.HealthCheckService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PhotoHealthCheckController.class)
@WebMvcTestSupport
class PhotoHealthCheckControllerTest {

    private static final Instant CHECKED_AT = Instant.parse("2026-10-18T09:10:00Z");
    private static final List<HealthCheckItem> ITEMS = List.of(
            new HealthCheckItem(HealthCheckKey.RETAINED_SHED, HealthCheckResult.ATTENTION, "오른쪽 뒷발 발가락에 허물이 남아 보여요"),
            new HealthCheckItem(HealthCheckKey.EYES, HealthCheckResult.GOOD, null));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Test
    @WithMockUserId
    void 건강_체크하면_명세_형식으로_돌려준다() throws Exception {
        given(healthCheckService.check(301L)).willReturn(
                new HealthCheckResponse(55L, CHECKED_AT, ITEMS, HealthCheckResponse.DISCLAIMER));

        mockMvc.perform(post("/api/v1/photos/301/health-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkId").value(55))
                .andExpect(jsonPath("$.data.checkedAt").value("2026-10-18T09:10:00Z"))
                .andExpect(jsonPath("$.data.items[0].key").value("RETAINED_SHED"))
                .andExpect(jsonPath("$.data.items[0].result").value("ATTENTION"))
                .andExpect(jsonPath("$.data.items[0].note").value("오른쪽 뒷발 발가락에 허물이 남아 보여요"))
                .andExpect(jsonPath("$.data.items[1].result").value("GOOD"))
                .andExpect(jsonPath("$.data.disclaimer").value(HealthCheckResponse.DISCLAIMER));
    }

    @Test
    @WithMockUserId
    void 파충류가_없으면_422_횟수를_다_쓰면_429() throws Exception {
        given(healthCheckService.check(301L)).willThrow(new BusinessException(ErrorCode.NO_REPTILE_IN_PHOTO));
        given(healthCheckService.check(302L)).willThrow(new BusinessException(ErrorCode.AI_LIMIT_EXCEEDED));

        mockMvc.perform(post("/api/v1/photos/301/health-check"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_REPTILE_IN_PHOTO"));
        mockMvc.perform(post("/api/v1/photos/302/health-check"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AI_LIMIT_EXCEEDED"));
    }

    @Test
    @WithMockUserId
    void 결과_목록을_명세_형식으로_돌려준다() throws Exception {
        given(healthCheckService.getChecks(eq(12L), isNull(), anyInt())).willReturn(new HealthCheckListResponse(
                List.of(new HealthCheckSummaryResponse(55L, 301L, "https://files/301_thumb.jpg", CHECKED_AT, ITEMS)),
                null));

        mockMvc.perform(get("/api/v1/health/checks").param("animalId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checks[0].checkId").value(55))
                .andExpect(jsonPath("$.data.checks[0].photoId").value(301))
                .andExpect(jsonPath("$.data.checks[0].thumbnailUrl").value("https://files/301_thumb.jpg"))
                .andExpect(jsonPath("$.data.checks[0].items[0].key").value("RETAINED_SHED"))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    @WithMockUserId
    void 목록에_animalId가_없으면_400() throws Exception {
        mockMvc.perform(get("/api/v1/health/checks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/photos/301/health-check"))
                .andExpect(status().isUnauthorized());
    }
}
