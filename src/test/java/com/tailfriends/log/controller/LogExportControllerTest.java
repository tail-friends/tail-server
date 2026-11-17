package com.tailfriends.log.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.log.service.LogExportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogExportController.class)
@WebMvcTestSupport
class LogExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogExportService logExportService;

    @Test
    @WithMockUserId(7L)
    void CSV_파일로_내려준다() throws Exception {
        byte[] csv = "개체명\r\n".getBytes(StandardCharsets.UTF_8);
        given(logExportService.exportCsv(12L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).willReturn(csv);

        mockMvc.perform(get("/api/v1/users/me/export")
                        .param("animalId", "12").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".csv")))
                .andExpect(content().bytes(csv));
    }

    @Test
    @WithMockUserId(7L)
    void 파라미터_없이도_내보낸다() throws Exception {
        given(logExportService.exportCsv(null, null, null)).willReturn(new byte[0]);

        mockMvc.perform(get("/api/v1/users/me/export")).andExpect(status().isOk());
        verify(logExportService).exportCsv(null, null, null);
    }

    @Test
    @WithMockUserId(7L)
    void 날짜_형식이_틀리면_400() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/export").param("from", "2026/10/01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        verify(logExportService, never()).exportCsv(any(), any(), any());
    }

    @Test
    @WithMockUserId(7L)
    void 실패는_JSON_에러로_돌려준다() throws Exception {
        given(logExportService.exportCsv(99L, null, null)).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/me/export").param("animalId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/export")).andExpect(status().isUnauthorized());
    }
}
