package com.tailfriends.animal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.dto.AnimalCreateResponse;
import com.tailfriends.animal.dto.AnimalResponse;
import com.tailfriends.animal.dto.FeedResponse;
import com.tailfriends.animal.service.AnimalService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.dto.AlertResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnimalController.class)
@WebMvcTestSupport
class AnimalControllerTest {

    private static final String CREATE_BODY = """
            { "name": "망고", "speciesId": 1, "speciesName": null, "morph": "탠저린",
              "sex": "FEMALE", "hatchDate": "2025-06-20", "feedIntervalDays": 4, "memo": "핫존 32℃" }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @Test
    @WithMockUserId(7L)
    void 개체를_등록하면_201과_ID를_돌려준다() throws Exception {
        given(animalService.create(eq(7L), any())).willReturn(new AnimalCreateResponse(12L));

        mockMvc.perform(post("/api/v1/animals").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12));
    }

    @Test
    @WithMockUserId
    void 급여_주기가_60일을_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/animals").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"feedIntervalDays\": 61 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void 성별이_정해진_값이_아니면_400() throws Exception {
        mockMvc.perform(post("/api/v1/animals").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"망고\", \"speciesId\": 1, \"sex\": \"BOY\", \"feedIntervalDays\": 4 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(7L)
    void 개체_상세를_명세_형식으로_돌려준다() throws Exception {
        given(animalService.getAnimal(7L, 12L)).willReturn(new AnimalResponse(
                12L, "망고", "레오파드 게코", "탠저린", Sex.FEMALE, LocalDate.of(2025, 6, 20), 4, "핫존 32℃",
                new FeedResponse(FeedStatus.UPCOMING, LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 18), 1),
                List.of(new AlertResponse(77L, 12L, AlertType.WEIGHT_LOSS_ALERT, "최근 30일 동안 체중이 12% 줄었어요",
                        AlertStatus.OPEN, Instant.parse("2026-10-18T09:00:00Z")))));

        mockMvc.perform(get("/api/v1/animals/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.species").value("레오파드 게코"))
                .andExpect(jsonPath("$.data.sex").value("FEMALE"))
                .andExpect(jsonPath("$.data.hatchDate").value("2025-06-20"))
                .andExpect(jsonPath("$.data.feedIntervalDays").value(4))
                .andExpect(jsonPath("$.data.feed.status").value("UPCOMING"))
                .andExpect(jsonPath("$.data.feed.lastDate").value("2026-10-14"))
                .andExpect(jsonPath("$.data.feed.nextDate").value("2026-10-18"))
                .andExpect(jsonPath("$.data.alerts[0].type").value("WEIGHT_LOSS_ALERT"))
                .andExpect(jsonPath("$.data.alerts[0].createdAt").value("2026-10-18T09:00:00Z"));
    }

    @Test
    @WithMockUserId(7L)
    void 없거나_남의_개체면_404() throws Exception {
        given(animalService.getAnimal(7L, 99L)).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));

        mockMvc.perform(get("/api/v1/animals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    @WithMockUserId(7L)
    void 개체를_삭제하면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/animals/12"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(animalService).delete(7L, 12L);
    }

    @Test
    void 개체_API는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/v1/animals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
