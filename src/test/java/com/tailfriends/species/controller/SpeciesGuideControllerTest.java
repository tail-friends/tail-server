package com.tailfriends.species.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.species.domain.GuideSection;
import com.tailfriends.species.dto.SpeciesGuideResponse;
import com.tailfriends.species.service.SpeciesGuideService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpeciesGuideController.class)
@WebMvcTestSupport
class SpeciesGuideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeciesGuideService speciesGuideService;

    @Test
    void 인증_없이_가이드를_조회한다() throws Exception {
        given(speciesGuideService.getGuide(1L, GuideSection.HEALTH)).willReturn(new SpeciesGuideResponse(1L, "레오파드 게코",
                List.of(new SpeciesGuideResponse.Section(GuideSection.HEALTH, "건강", "탈피 부전 주의",
                        LocalDate.of(2026, 10, 21)))));

        mockMvc.perform(get("/api/v1/species/1/guide").param("section", "HEALTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.speciesId").value(1))
                .andExpect(jsonPath("$.data.name").value("레오파드 게코"))
                .andExpect(jsonPath("$.data.sections[0].key").value("HEALTH"))
                .andExpect(jsonPath("$.data.sections[0].title").value("건강"))
                .andExpect(jsonPath("$.data.sections[0].body").value("탈피 부전 주의"))
                .andExpect(jsonPath("$.data.sections[0].updatedAt").value("2026-10-21"));
    }

    @Test
    void 없는_section이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/species/1/guide").param("section", "SLEEP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 없는_종이면_404() throws Exception {
        given(speciesGuideService.getGuide(99L, null)).willThrow(new BusinessException(ErrorCode.SPECIES_NOT_FOUND));

        mockMvc.perform(get("/api/v1/species/99/guide"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPECIES_NOT_FOUND"));
    }
}
