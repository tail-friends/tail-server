package com.tailfriends.animal.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.service.HealthReportService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthReportController.class)
@WebMvcTestSupport
class HealthReportControllerTest {

    private static final byte[] PDF = "%PDF-1.4".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthReportService healthReportService;

    @Test
    @WithMockUserId(7L)
    void 기본은_최근_3개월_PDF를_내려준다() throws Exception {
        given(healthReportService.createReport(12L, 3)).willReturn(PDF);

        mockMvc.perform(get("/api/v1/animals/12/report"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"health-report-12.pdf\"")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    @WithMockUserId(7L)
    void months로_기간을_정한다() throws Exception {
        given(healthReportService.createReport(12L, 12)).willReturn(PDF);

        mockMvc.perform(get("/api/v1/animals/12/report").param("months", "12")).andExpect(status().isOk());
        verify(healthReportService).createReport(12L, 12);
    }

    @Test
    @WithMockUserId(7L)
    void months가_1에서_12_밖이면_400() throws Exception {
        for (String months : new String[] {"0", "13", "abc"}) {
            mockMvc.perform(get("/api/v1/animals/12/report").param("months", months))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }
        verify(healthReportService, never()).createReport(anyLong(), anyInt());
    }

    @Test
    @WithMockUserId(7L)
    void 내_개체가_아니면_404_JSON_에러() throws Exception {
        given(healthReportService.createReport(99L, 3)).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));

        mockMvc.perform(get("/api/v1/animals/99/report"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/animals/12/report")).andExpect(status().isUnauthorized());
    }
}
