package com.tailfriends.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.admin.dto.AdminStatsResponse;
import com.tailfriends.admin.dto.AdminStatsResponse.Daily;
import com.tailfriends.admin.service.AdminStatsService;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminStatsController.class)
@WebMvcTestSupport
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStatsService adminStatsService;

    @Test
    @WithMockUserId(role = "ADMIN")
    void 기본은_최근_7일() throws Exception {
        given(adminStatsService.getStats(7)).willReturn(new AdminStatsResponse(120, 340,
                List.of(new Daily(LocalDate.of(2026, 10, 20), 1, 2))));

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(120))
                .andExpect(jsonPath("$.data.totalAnimals").value(340))
                .andExpect(jsonPath("$.data.daily[0].date").value("2026-10-20"))
                .andExpect(jsonPath("$.data.daily[0].newAnimals").value(2));
    }

    @Test
    @WithMockUserId(role = "ADMIN")
    void days가_범위를_벗어나면_400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats").param("days", "91"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void 일반_사용자는_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isForbidden());
    }
}
