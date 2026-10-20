package com.tailfriends.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.dto.AlertListResponse;
import com.tailfriends.health.dto.AlertResponse;
import com.tailfriends.health.service.HealthAlertService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthAlertController.class)
@WebMvcTestSupport
class HealthAlertControllerTest {

    private static final AlertResponse ALERT = new AlertResponse(77L, 12L, AlertType.WEIGHT_LOSS_ALERT,
            "최근 30일 동안 체중이 12% 줄었어요", AlertStatus.OPEN, Instant.parse("2026-10-18T09:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthAlertService healthAlertService;

    @Test
    @WithMockUserId(7L)
    void 경고_목록을_명세_형식으로_돌려준다() throws Exception {
        given(healthAlertService.getAlerts(7L, null, AlertStatus.OPEN, null, 20))
                .willReturn(new AlertListResponse(List.of(ALERT), null));

        mockMvc.perform(get("/api/v1/health/alerts").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alerts[0].id").value(77))
                .andExpect(jsonPath("$.data.alerts[0].animalId").value(12))
                .andExpect(jsonPath("$.data.alerts[0].type").value("WEIGHT_LOSS_ALERT"))
                .andExpect(jsonPath("$.data.alerts[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.alerts[0].createdAt").value("2026-10-18T09:00:00Z"))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
    }

    @Test
    @WithMockUserId
    void 상태가_정해진_값이_아니면_400() throws Exception {
        mockMvc.perform(get("/api/v1/health/alerts").param("status", "CLOSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(7L)
    void 경고를_확인_처리한다() throws Exception {
        given(healthAlertService.update(eq(7L), eq(77L), any())).willReturn(new AlertResponse(77L, 12L,
                AlertType.WEIGHT_LOSS_ALERT, ALERT.message(), AlertStatus.ACKNOWLEDGED, ALERT.createdAt()));

        mockMvc.perform(patch("/api/v1/health/alerts/77").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"ACKNOWLEDGED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
    }

    @Test
    @WithMockUserId
    void 없는_경고면_ALERT_NOT_FOUND() throws Exception {
        given(healthAlertService.update(any(), eq(999L), any())).willThrow(new BusinessException(ErrorCode.ALERT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/health/alerts/999").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"ACKNOWLEDGED\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ALERT_NOT_FOUND"));
    }

    @Test
    @WithMockUserId
    void 확인_처리_요청에_status가_없으면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/health/alerts/77").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
