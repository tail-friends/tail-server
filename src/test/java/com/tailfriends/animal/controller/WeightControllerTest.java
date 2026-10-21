package com.tailfriends.animal.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.dto.WeightGraphResponse;
import com.tailfriends.animal.service.WeightService;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.log.repository.WeightPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeightController.class)
@WebMvcTestSupport
class WeightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeightService weightService;

    @Test
    @WithMockUserId(7L)
    void 기간을_YYYY_MM_DD로_받아_명세_형식으로_돌려준다() throws Exception {
        given(weightService.getWeights(7L, 12L, LocalDate.of(2025, 10, 18), LocalDate.of(2026, 10, 18)))
                .willReturn(new WeightGraphResponse(List.of(
                        new WeightPoint(LocalDate.of(2026, 7, 5), new BigDecimal("48.0")),
                        new WeightPoint(LocalDate.of(2026, 10, 3), new BigDecimal("57.8")))));

        mockMvc.perform(get("/api/v1/animals/12/weights").param("from", "2025-10-18").param("to", "2026-10-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points[0].date").value("2026-07-05"))
                .andExpect(jsonPath("$.data.points[0].value").value(48.0))
                .andExpect(jsonPath("$.data.points[1].value").value(57.8));
    }

    @Test
    @WithMockUserId
    void 날짜_형식이_틀리면_400() throws Exception {
        mockMvc.perform(get("/api/v1/animals/12/weights").param("from", "2025/10/18"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
