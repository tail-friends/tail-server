package com.tailfriends.species.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.species.dto.SpeciesListResponse;
import com.tailfriends.species.dto.SpeciesResponse;
import com.tailfriends.species.service.SpeciesService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpeciesController.class)
@WebMvcTestSupport
class SpeciesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeciesService speciesService;

    @Test
    void 인증_없이_종_목록을_조회한다() throws Exception {
        given(speciesService.getSpeciesList()).willReturn(new SpeciesListResponse(List.of(
                new SpeciesResponse(1L, "레오파드 게코", "gecko", false, 4, null,
                        new SpeciesResponse.EnvRange(null, null, null), "desert"))));

        mockMvc.perform(get("/api/v1/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.species[0].id").value(1))
                .andExpect(jsonPath("$.data.species[0].name").value("레오파드 게코"))
                .andExpect(jsonPath("$.data.species[0].shape").value("gecko"))
                .andExpect(jsonPath("$.data.species[0].feedIntervalDays").value(4))
                .andExpect(jsonPath("$.data.species[0].shedIntervalDays").isEmpty())
                .andExpect(jsonPath("$.data.species[0].envRange.hot").isEmpty())
                .andExpect(jsonPath("$.data.species[0].defaultHome").value("desert"));
    }

    @Test
    void 종_경로의_GET_외_요청은_인증이_필요하다() throws Exception {
        mockMvc.perform(post("/api/v1/species"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
