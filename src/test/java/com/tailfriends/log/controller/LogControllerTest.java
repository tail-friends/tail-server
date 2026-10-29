package com.tailfriends.log.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.dto.FeedResponse;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.log.domain.LogType;
import com.tailfriends.log.domain.ShedStage;
import com.tailfriends.log.dto.LogCreateResponse;
import com.tailfriends.log.dto.LogIdResponse;
import com.tailfriends.log.dto.LogListResponse;
import com.tailfriends.log.dto.LogResponse;
import com.tailfriends.log.service.LogService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogController.class)
@WebMvcTestSupport
class LogControllerTest {

    private static final String FEED_BODY = """
            { "clientId": "8f1c2d3e-0000-4000-8000-000000000001", "animalId": 12, "type": "FEED",
              "date": "2026-10-18", "food": "귀뚜라미", "count": 6, "refused": false, "dusted": true, "memo": "" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogService logService;

    @Test
    @WithMockUserId(7L)
    void 기록을_작성하면_201과_ID를_돌려준다() throws Exception {
        given(logService.create(any())).willReturn(new LogCreateResponse(902L, 10, List.of(),
                new FeedResponse(FeedStatus.UPCOMING, LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 22), 2)));

        mockMvc.perform(post("/api/v1/logs").contentType(MediaType.APPLICATION_JSON).content(FEED_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(902))
                .andExpect(jsonPath("$.data.pointsEarned").value(10))
                .andExpect(jsonPath("$.data.newAlerts").isArray())
                .andExpect(jsonPath("$.data.feed.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.feed.nextDate").value("2026-10-22"));
    }

    @Test
    @WithMockUserId
    void 같은_clientId로_다시_보내면_409와_기존_기록_ID를_돌려준다() throws Exception {
        given(logService.create(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_CLIENT_ID,
                ErrorCode.DUPLICATE_CLIENT_ID.getMessage(), new LogIdResponse(880L)));

        mockMvc.perform(post("/api/v1/logs").contentType(MediaType.APPLICATION_JSON).content(FEED_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.id").value(880))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CLIENT_ID"));
    }

    @Test
    @WithMockUserId
    void 종류가_정해진_값이_아니면_400() throws Exception {
        mockMvc.perform(post("/api/v1/logs").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": 12, \"type\": \"BATH\", \"date\": \"2026-10-18\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void 체중이_0_이하이거나_소수_둘째_자리까지_있으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/logs").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": 12, \"type\": \"WEIGHT\", \"date\": \"2026-10-18\", \"value\": 0 }"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/logs").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": 12, \"type\": \"WEIGHT\", \"date\": \"2026-10-18\", \"value\": 52.35 }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId(7L)
    void 기록_목록은_종류에_해당하는_필드만_내려준다() throws Exception {
        given(logService.getLogs(12L, null, null, 20)).willReturn(new LogListResponse(List.of(
                new LogResponse(901L, LogType.FEED, LocalDate.of(2026, 10, 14), "밀웜", 5, false, true,
                        null, null, null, null, null, null, null, null, null),
                new LogResponse(880L, LogType.SHED, LocalDate.of(2026, 9, 22), null, null, null, null,
                        ShedStage.SHED, false, null, null, null, null, null, null, "꼬리 끝 허물")), 870L));

        mockMvc.perform(get("/api/v1/logs").param("animalId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logs[0].food").value("밀웜"))
                .andExpect(jsonPath("$.data.logs[0].date").value("2026-10-14"))
                .andExpect(jsonPath("$.data.logs[0].memo").isEmpty())
                .andExpect(jsonPath("$.data.logs[0].stage").doesNotExist())
                .andExpect(jsonPath("$.data.logs[1].stage").value("SHED"))
                .andExpect(jsonPath("$.data.logs[1].food").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").value(870));
    }

    @Test
    @WithMockUserId
    void 목록에_animalId가_없으면_400() throws Exception {
        mockMvc.perform(get("/api/v1/logs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void 목록_size가_범위를_벗어나면_400() throws Exception {
        mockMvc.perform(get("/api/v1/logs").param("animalId", "12").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(7L)
    void 기록을_삭제하면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/logs/902"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(logService).delete(902L);
    }

    @Test
    @WithMockUserId
    void 없는_기록을_삭제하면_LOG_NOT_FOUND() throws Exception {
        willThrow(new BusinessException(ErrorCode.LOG_NOT_FOUND))
                .given(logService).delete(eq(999L));

        mockMvc.perform(delete("/api/v1/logs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("LOG_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/logs").param("animalId", "12"))
                .andExpect(status().isUnauthorized());
    }
}
