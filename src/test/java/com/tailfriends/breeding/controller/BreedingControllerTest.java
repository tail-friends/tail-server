package com.tailfriends.breeding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.breeding.domain.BreedingStatus;
import com.tailfriends.breeding.dto.BreedingCreateResponse;
import com.tailfriends.breeding.dto.BreedingListResponse;
import com.tailfriends.breeding.dto.BreedingResponse;
import com.tailfriends.breeding.dto.ClutchResponse;
import com.tailfriends.breeding.dto.HatchlingsCreateResponse;
import com.tailfriends.breeding.service.BreedingService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BreedingController.class)
@WebMvcTestSupport
class BreedingControllerTest {

    private static final BreedingResponse BREEDING = new BreedingResponse(4L, 15L, "망고", null, "분양처 암컷",
            LocalDate.of(2026, 3, 2), BreedingStatus.INCUBATING, "첫 메이팅",
            List.of(new ClutchResponse(LocalDate.of(2026, 4, 10), 2, new BigDecimal("30.0"), LocalDate.of(2026, 6, 5))),
            List.of(31L));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BreedingService breedingService;

    @Test
    @WithMockUserId(7L)
    void 번식_기록_목록을_명세_형식으로_돌려준다() throws Exception {
        given(breedingService.getBreedings()).willReturn(new BreedingListResponse(List.of(BREEDING)));

        mockMvc.perform(get("/api/v1/breeding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.breedings[0].id").value(4))
                .andExpect(jsonPath("$.data.breedings[0].maleId").value(15))
                .andExpect(jsonPath("$.data.breedings[0].maleName").value("망고"))
                .andExpect(jsonPath("$.data.breedings[0].femaleId").isEmpty())
                .andExpect(jsonPath("$.data.breedings[0].femaleName").value("분양처 암컷"))
                .andExpect(jsonPath("$.data.breedings[0].pairedDate").value("2026-03-02"))
                .andExpect(jsonPath("$.data.breedings[0].status").value("INCUBATING"))
                .andExpect(jsonPath("$.data.breedings[0].memo").value("첫 메이팅"))
                .andExpect(jsonPath("$.data.breedings[0].clutches[0].laidDate").value("2026-04-10"))
                .andExpect(jsonPath("$.data.breedings[0].clutches[0].eggCount").value(2))
                .andExpect(jsonPath("$.data.breedings[0].clutches[0].incubationTemp").value(30.0))
                .andExpect(jsonPath("$.data.breedings[0].clutches[0].expectedHatchDate").value("2026-06-05"))
                .andExpect(jsonPath("$.data.breedings[0].hatchlingIds[0]").value(31));
    }

    @Test
    @WithMockUserId(7L)
    void 생성하면_201과_ID를_돌려준다() throws Exception {
        given(breedingService.create(any())).willReturn(new BreedingCreateResponse(4L));

        mockMvc.perform(post("/api/v1/breeding").contentType(MediaType.APPLICATION_JSON).content("""
                        { "maleId": 15, "femaleName": "분양처 암컷", "pairedDate": "2026-03-02", "memo": "" }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(4));
    }

    @Test
    @WithMockUserId(7L)
    void 생성에_메이팅일이_없거나_이름이_길면_400() throws Exception {
        mockMvc.perform(post("/api/v1/breeding").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"maleId\": 15, \"femaleId\": 12 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/breeding").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"maleName\": \"%s\", \"femaleId\": 12, \"pairedDate\": \"2026-03-02\" }"
                                .formatted("수".repeat(31))))
                .andExpect(status().isBadRequest());

        verify(breedingService, never()).create(any());
    }

    @Test
    @WithMockUserId(7L)
    void 수정하면_수정된_번식_기록을_돌려주고_없으면_404() throws Exception {
        given(breedingService.update(eq(4L), any())).willReturn(BREEDING);
        given(breedingService.update(eq(99L), any())).willThrow(new BusinessException(ErrorCode.BREEDING_NOT_FOUND));
        String body = """
                { "status": "INCUBATING",
                  "clutches": [ { "laidDate": "2026-04-10", "eggCount": 2, "incubationTemp": 30.0,
                                  "expectedHatchDate": "2026-06-05" } ] }
                """;

        mockMvc.perform(patch("/api/v1/breeding/4").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INCUBATING"))
                .andExpect(jsonPath("$.data.clutches[0].eggCount").value(2));
        mockMvc.perform(patch("/api/v1/breeding/99").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BREEDING_NOT_FOUND"));
    }

    @Test
    @WithMockUserId(7L)
    void 산란_기록의_알_개수나_온도가_범위를_벗어나거나_상태가_틀리면_400() throws Exception {
        for (String body : List.of(
                "{ \"clutches\": [ { \"laidDate\": \"2026-04-10\", \"eggCount\": 0 } ] }",
                "{ \"clutches\": [ { \"laidDate\": \"2026-04-10\", \"eggCount\": 100 } ] }",
                "{ \"clutches\": [ { \"eggCount\": 2 } ] }",
                "{ \"clutches\": [ { \"laidDate\": \"2026-04-10\", \"eggCount\": 2, \"incubationTemp\": 41.0 } ] }",
                "{ \"clutches\": [ null ] }",
                "{ \"status\": \"LAID\" }")) {
            mockMvc.perform(patch("/api/v1/breeding/4").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }

        verify(breedingService, never()).update(any(), any());
    }

    @Test
    @WithMockUserId(7L)
    void 산란_기록은_20개까지() throws Exception {
        String clutch = "{ \"laidDate\": \"2026-04-10\", \"eggCount\": 2 }";
        String body = "{ \"clutches\": [ %s ] }".formatted(String.join(", ", Collections.nCopies(21, clutch)));

        mockMvc.perform(patch("/api/v1/breeding/4").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId(7L)
    void 삭제하면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/breeding/4"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(breedingService).delete(4L);
    }

    @Test
    @WithMockUserId(7L)
    void 해칭_개체를_등록하면_201과_개체_ID를_돌려준다() throws Exception {
        given(breedingService.registerHatchlings(eq(4L), any()))
                .willReturn(new HatchlingsCreateResponse(List.of(31L, 32L)));

        mockMvc.perform(post("/api/v1/breeding/4/hatchlings").contentType(MediaType.APPLICATION_JSON).content("""
                        { "hatchDate": "2026-06-03",
                          "hatchlings": [ { "name": "망고 2세 A", "morph": "탠저린" }, { "name": "망고 2세 B" } ] }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.animalIds[0]").value(31))
                .andExpect(jsonPath("$.data.animalIds[1]").value(32));
    }

    @Test
    @WithMockUserId(7L)
    void 해칭_개체가_없거나_이름이_없거나_해칭일이_없으면_400() throws Exception {
        for (String body : List.of(
                "{ \"hatchDate\": \"2026-06-03\", \"hatchlings\": [] }",
                "{ \"hatchDate\": \"2026-06-03\", \"hatchlings\": [ { \"name\": \" \" } ] }",
                "{ \"hatchlings\": [ { \"name\": \"새끼\" } ] }")) {
            mockMvc.perform(post("/api/v1/breeding/4/hatchlings").contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }

        verify(breedingService, never()).registerHatchlings(any(), any());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/breeding"))
                .andExpect(status().isUnauthorized());
    }
}
