package com.tailfriends.user.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.user.domain.AlertRules;
import com.tailfriends.user.domain.TempUnit;
import com.tailfriends.user.dto.SettingsResponse;
import com.tailfriends.user.dto.SettingsUpdateRequest;
import com.tailfriends.user.dto.SettingsUpdateRequest.AlertRulesUpdate;
import com.tailfriends.user.service.UserSettingsService;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserSettingsController.class)
@WebMvcTestSupport
class UserSettingsControllerTest {

    private static final SettingsResponse SETTINGS =
            new SettingsResponse(LocalTime.of(8, 30), TempUnit.F, false, new AlertRules(5, 10, 30, 2), true);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @Test
    @WithMockUserId(1L)
    void 설정을_명세_형식으로_돌려준다() throws Exception {
        given(userSettingsService.getSettings()).willReturn(SETTINGS);

        mockMvc.perform(get("/api/v1/users/me/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifyTime").value("08:30"))
                .andExpect(jsonPath("$.data.tempUnit").value("F"))
                .andExpect(jsonPath("$.data.keepOriginalDefault").value(false))
                .andExpect(jsonPath("$.data.alertRules.refusalCount").value(5))
                .andExpect(jsonPath("$.data.alertRules.weightLossPercent").value(10))
                .andExpect(jsonPath("$.data.alertRules.weightLossDays").value(30))
                .andExpect(jsonPath("$.data.alertRules.looseStoolCount").value(2))
                .andExpect(jsonPath("$.data.pushEnabled").value(true));
    }

    @Test
    @WithMockUserId(1L)
    void 보낸_필드만_바꾼다() throws Exception {
        given(userSettingsService.updateSettings(eq(new SettingsUpdateRequest(
                LocalTime.of(8, 30), TempUnit.F, null, new AlertRulesUpdate(5, null, null, null), null))))
                .willReturn(SETTINGS);

        mockMvc.perform(patch("/api/v1/users/me/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"notifyTime\": \"08:30\", \"tempUnit\": \"F\", \"alertRules\": { \"refusalCount\": 5 } }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notifyTime").value("08:30"));
    }

    @Test
    @WithMockUserId(1L)
    void 경고_기준이_범위를_벗어나면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"alertRules\": { \"weightLossDays\": 365 } }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(1L)
    void 알림_시간_형식이_틀리면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"notifyTime\": \"25:00\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/settings"))
                .andExpect(status().isUnauthorized());
    }
}
