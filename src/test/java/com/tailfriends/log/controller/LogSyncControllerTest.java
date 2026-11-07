package com.tailfriends.log.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.log.dto.LogSyncResponse;
import com.tailfriends.log.dto.LogSyncResult;
import com.tailfriends.log.service.LogSyncService;
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

@WebMvcTest(LogSyncController.class)
@WebMvcTestSupport
class LogSyncControllerTest {

    private static final UUID CLIENT_A = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    private static final UUID CLIENT_B = UUID.fromString("c2000000-0000-4000-8000-000000000002");
    private static final UUID CLIENT_C = UUID.fromString("c3000000-0000-4000-8000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogSyncService logSyncService;

    @Test
    @WithMockUserId
    void 항목별_결과를_200으로_돌려준다() throws Exception {
        given(logSyncService.sync(any())).willReturn(new LogSyncResponse(List.of(
                LogSyncResult.created(CLIENT_A, 905L),
                LogSyncResult.duplicate(CLIENT_B, 700L),
                LogSyncResult.failed(CLIENT_C, ErrorCode.ANIMAL_NOT_FOUND, "개체를 찾을 수 없어요"))));

        mockMvc.perform(post("/api/v1/logs/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "logs": [
                                  { "clientId": "%s", "animalId": 12, "type": "WEIGHT", "date": "2026-10-17", "value": 58.1 },
                                  { "clientId": "%s", "animalId": 12, "type": "CLEAN", "date": "2026-10-17" },
                                  { "clientId": "%s", "animalId": 99, "type": "NOTE", "date": "2026-10-17", "memo": "메모" }
                                ] }
                                """.formatted(CLIENT_A, CLIENT_B, CLIENT_C)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.results[0].id").value(905))
                .andExpect(jsonPath("$.data.results[1].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.data.results[1].id").value(700))
                .andExpect(jsonPath("$.data.results[2].status").value("FAILED"))
                .andExpect(jsonPath("$.data.results[2].error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    @WithMockUserId
    void 기록이_없거나_200건을_넘으면_400() throws Exception {
        String logs201 = IntStream.rangeClosed(1, 201)
                .mapToObj(i -> "{ \"animalId\": 1, \"type\": \"CLEAN\", \"date\": \"2026-10-17\" }")
                .collect(Collectors.joining(","));

        mockMvc.perform(post("/api/v1/logs/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"logs\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/logs/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"logs\": [" + logs201 + "] }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/logs/sync").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"logs\": [] }"))
                .andExpect(status().isUnauthorized());
    }
}
