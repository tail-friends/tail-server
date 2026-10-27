package com.tailfriends.log.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.log.dto.LogBatchResponse;
import com.tailfriends.log.service.LogBatchService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogBatchController.class)
@WebMvcTestSupport
class LogBatchControllerTest {

    private static final UUID BATCH_ID = UUID.fromString("7f2c0000-0000-4000-8000-000000000001");
    private static final String FEED_BODY = """
            { "type": "FEED", "date": "2026-10-18",
              "items": [
                { "clientId": "a1000000-0000-4000-8000-000000000001", "animalId": 12, "food": "귀뚜라미", "count": 6, "refused": false },
                { "clientId": "a2000000-0000-4000-8000-000000000002", "animalId": 15, "food": "해동 마우스", "count": 1, "refused": true }
              ] }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogBatchService logBatchService;

    @Test
    @WithMockUserId
    void 일괄_기록하면_201과_묶음_ID_기록_ID_되돌리기_기한을_돌려준다() throws Exception {
        given(logBatchService.create(any())).willReturn(
                new LogBatchResponse(BATCH_ID, List.of(903L, 904L), Instant.parse("2026-10-18T09:00:05Z")));

        mockMvc.perform(post("/api/v1/logs/batch").contentType(MediaType.APPLICATION_JSON).content(FEED_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.batchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.data.logIds[1]").value(904))
                .andExpect(jsonPath("$.data.undoUntil").value("2026-10-18T09:00:05Z"));
    }

    @Test
    @WithMockUserId
    void 항목이_없거나_30개를_넘으면_400() throws Exception {
        String items31 = IntStream.rangeClosed(1, 31)
                .mapToObj(i -> "{ \"animalId\": " + i + " }")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/v1/logs/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"CLEAN\", \"date\": \"2026-10-18\", \"items\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/logs/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"CLEAN\", \"date\": \"2026-10-18\", \"items\": [" + items31 + "] }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId
    void 항목에_개체가_없거나_체중이_0_이하면_400() throws Exception {
        mockMvc.perform(post("/api/v1/logs/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"CLEAN\", \"date\": \"2026-10-18\", \"items\": [ { \"full\": true } ] }"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/logs/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"type\": \"WEIGHT\", \"date\": \"2026-10-18\", \"items\": [ { \"animalId\": 12, \"value\": 0 } ] }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId
    void 되돌리면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/logs/batch/" + BATCH_ID))
                .andExpect(status().isNoContent());
        verify(logBatchService).undo(BATCH_ID);
    }

    @Test
    @WithMockUserId
    void 되돌리기_시간이_지났으면_409_UNDO_EXPIRED() throws Exception {
        willThrow(new BusinessException(ErrorCode.UNDO_EXPIRED)).given(logBatchService).undo(BATCH_ID);

        mockMvc.perform(delete("/api/v1/logs/batch/" + BATCH_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("UNDO_EXPIRED"));
    }

    @Test
    @WithMockUserId
    void 묶음_ID가_UUID가_아니면_400() throws Exception {
        mockMvc.perform(delete("/api/v1/logs/batch/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/logs/batch").contentType(MediaType.APPLICATION_JSON).content(FEED_BODY))
                .andExpect(status().isUnauthorized());
    }
}
