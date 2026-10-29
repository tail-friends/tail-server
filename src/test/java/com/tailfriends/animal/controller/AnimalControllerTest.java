package com.tailfriends.animal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.domain.FeedStatus;
import com.tailfriends.animal.domain.Mood;
import com.tailfriends.animal.domain.Sex;
import com.tailfriends.animal.domain.ShedSummary;
import com.tailfriends.animal.dto.AnimalCreateResponse;
import com.tailfriends.animal.dto.AnimalHomeRequest;
import com.tailfriends.animal.dto.AnimalHomeResponse;
import com.tailfriends.animal.dto.AnimalResponse;
import com.tailfriends.animal.dto.CharacterResponse;
import com.tailfriends.animal.dto.CharacterSummaryResponse;
import com.tailfriends.animal.dto.FeedResponse;
import com.tailfriends.animal.dto.WeightSummaryResponse;
import com.tailfriends.animal.service.AnimalService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.health.domain.AlertStatus;
import com.tailfriends.health.domain.AlertType;
import com.tailfriends.health.dto.AlertResponse;
import com.tailfriends.species.domain.Home;
import java.math.BigDecimal;
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
        given(animalService.create(any())).willReturn(new AnimalCreateResponse(12L));

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
        given(animalService.getAnimal(12L)).willReturn(new AnimalResponse(
                12L, "망고", "레오파드 게코", "탠저린", Sex.FEMALE, LocalDate.of(2025, 6, 20), 4, "핫존 32℃",
                new FeedResponse(FeedStatus.UPCOMING, LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 18), 1),
                new ShedSummary(LocalDate.of(2026, 9, 22), false, 32, LocalDate.of(2026, 10, 24), false),
                new WeightSummaryResponse(new BigDecimal("57.8"), new BigDecimal("2.8")),
                new CharacterSummaryResponse("gecko", "spots",
                        new CharacterResponse.Colors("#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B"), "desert"),
                null,
                List.of(new AlertResponse(77L, 12L, AlertType.WEIGHT_LOSS_ALERT, "최근 30일 동안 체중이 12% 줄었어요",
                        AlertStatus.OPEN, Instant.parse("2026-10-18T09:00:00Z"))),
                Mood.UNWELL));

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
                .andExpect(jsonPath("$.data.cooling").isEmpty())
                .andExpect(jsonPath("$.data.alerts[0].type").value("WEIGHT_LOSS_ALERT"))
                .andExpect(jsonPath("$.data.alerts[0].createdAt").value("2026-10-18T09:00:00Z"))
                .andExpect(jsonPath("$.data.shed.lastDate").value("2026-09-22"))
                .andExpect(jsonPath("$.data.shed.complete").value(false))
                .andExpect(jsonPath("$.data.shed.avgIntervalDays").value(32))
                .andExpect(jsonPath("$.data.shed.predictedDate").value("2026-10-24"))
                .andExpect(jsonPath("$.data.shed.inBluePhase").value(false))
                .andExpect(jsonPath("$.data.weight.latest").value(57.8))
                .andExpect(jsonPath("$.data.weight.diff").value(2.8))
                .andExpect(jsonPath("$.data.character.shape").value("gecko"))
                .andExpect(jsonPath("$.data.character.colors.body").value("#E0A33A"))
                .andExpect(jsonPath("$.data.character.home").value("desert"))
                .andExpect(jsonPath("$.data.mood").value("UNWELL"));
    }

    @Test
    @WithMockUserId(7L)
    void 없거나_남의_개체면_404() throws Exception {
        given(animalService.getAnimal(99L)).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));

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

        verify(animalService).delete(12L);
    }

    @Test
    @WithMockUserId(7L)
    void 평소_사육장을_바꾸면_소문자로_돌려주고_대문자도_받는다() throws Exception {
        given(animalService.changeHome(eq(12L), any())).willReturn(new AnimalHomeResponse("forest"));

        mockMvc.perform(patch("/api/v1/animals/12/home").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"home\": \"FOREST\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.home").value("forest"));

        verify(animalService).changeHome(12L, new AnimalHomeRequest(Home.FOREST));
    }

    @Test
    @WithMockUserId(7L)
    void 평소_사육장이_없거나_테마_5개가_아니면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/animals/12/home").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(patch("/api/v1/animals/12/home").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"home\": \"beach\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(patch("/api/v1/animals/12/home").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"home\": 3 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(7L)
    void 남의_개체의_평소_사육장을_바꾸면_404() throws Exception {
        given(animalService.changeHome(eq(99L), any())).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/animals/99/home").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"home\": \"forest\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    void 개체_API는_인증이_필요하다() throws Exception {
        mockMvc.perform(get("/api/v1/animals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
