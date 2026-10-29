package com.tailfriends.health.controller;

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

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.health.dto.VetVisitCreateResponse;
import com.tailfriends.health.dto.VetVisitListResponse;
import com.tailfriends.health.dto.VetVisitResponse;
import com.tailfriends.health.service.VetVisitService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VetVisitController.class)
@WebMvcTestSupport
class VetVisitControllerTest {

    private static final VetVisitResponse VISIT = new VetVisitResponse(8L, "제주 특수동물병원",
            LocalDate.of(2026, 10, 2), "부분 탈피", "탈피 부전", "온욕 주 2회", LocalDate.of(2026, 10, 16));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VetVisitService vetVisitService;

    @Test
    @WithMockUserId(7L)
    void 개체의_진료_기록_목록을_명세_형식으로_돌려준다() throws Exception {
        given(vetVisitService.getVisits(12L)).willReturn(new VetVisitListResponse(List.of(VISIT)));

        mockMvc.perform(get("/api/v1/health/vet-visits").param("animalId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visits[0].id").value(8))
                .andExpect(jsonPath("$.data.visits[0].hospital").value("제주 특수동물병원"))
                .andExpect(jsonPath("$.data.visits[0].visitDate").value("2026-10-02"))
                .andExpect(jsonPath("$.data.visits[0].prescription").value("온욕 주 2회"))
                .andExpect(jsonPath("$.data.visits[0].nextVisitDate").value("2026-10-16"));
    }

    @Test
    @WithMockUserId(7L)
    void 목록에_animalId가_없으면_400() throws Exception {
        mockMvc.perform(get("/api/v1/health/vet-visits"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(7L)
    void 작성하면_201과_ID_투약_일정_ID를_돌려준다() throws Exception {
        given(vetVisitService.create(any())).willReturn(new VetVisitCreateResponse(8L, 41L));

        mockMvc.perform(post("/api/v1/health/vet-visits").contentType(MediaType.APPLICATION_JSON).content("""
                        { "animalId": 12, "hospital": "제주 특수동물병원", "visitDate": "2026-10-02",
                          "createMedicationSchedule": { "name": "온욕",
                            "repeat": { "type": "WEEKDAYS", "days": ["MON", "THU"] }, "endDate": "2026-10-31" } }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.scheduleId").value(41));
    }

    @Test
    @WithMockUserId(7L)
    void 필수값이_없거나_길이를_넘거나_투약_일정_이름이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/health/vet-visits").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": 12 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/health/vet-visits").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"animalId\": 12, \"visitDate\": \"2026-10-02\", \"hospital\": \"%s\" }"
                                .formatted("병".repeat(41))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/health/vet-visits").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "animalId": 12, "visitDate": "2026-10-02",
                                  "createMedicationSchedule": { "repeat": { "type": "EVERY_N_DAYS", "n": 2 } } }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId(7L)
    void 수정하면_수정된_진료_기록을_돌려주고_없으면_404() throws Exception {
        given(vetVisitService.update(eq(8L), any())).willReturn(VISIT);
        given(vetVisitService.update(eq(99L), any())).willThrow(new BusinessException(ErrorCode.VISIT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/health/vet-visits/8").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"diagnosis\": \"탈피 부전\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diagnosis").value("탈피 부전"));
        mockMvc.perform(patch("/api/v1/health/vet-visits/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"diagnosis\": \"탈피 부전\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VISIT_NOT_FOUND"));
    }

    @Test
    @WithMockUserId(7L)
    void 삭제하면_본문_없이_204() throws Exception {
        mockMvc.perform(delete("/api/v1/health/vet-visits/8"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(vetVisitService).delete(8L);
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/health/vet-visits").param("animalId", "12"))
                .andExpect(status().isUnauthorized());
    }
}
