package com.tailfriends.species.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class SpeciesIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 초기_데이터로_넣은_종_목록을_조회한다() throws Exception {
        mockMvc.perform(get("/api/v1/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.species", hasSize(10)))
                .andExpect(jsonPath("$.data.species[0].name").value("레오파드 게코"))
                .andExpect(jsonPath("$.data.species[0].shape").value("gecko"))
                .andExpect(jsonPath("$.data.species[0].feedIntervalDays").value(4))
                .andExpect(jsonPath("$.data.species[0].defaultHome").value("desert"))
                .andExpect(jsonPath("$.data.species[3].name").value("비어디드 드래곤"))
                .andExpect(jsonPath("$.data.species[3].spiky").value(true))
                .andExpect(jsonPath("$.data.species[0].category").value("reptile"))
                .andExpect(jsonPath("$.data.species[0].englishName").value("Leopard gecko"))
                .andExpect(jsonPath("$.data.species[0].scientificName").value("Eublepharis macularius"))
                .andExpect(jsonPath("$.data.species[0].aliases", hasSize(0)))
                .andExpect(jsonPath("$.data.species[4].scientificName").isEmpty())
                .andExpect(jsonPath("$.data.species[9].name").value("육지 거북"));
    }
}
