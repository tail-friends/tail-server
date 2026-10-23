package com.tailfriends.animal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.animal.domain.PatternType;
import com.tailfriends.animal.domain.RenderStyle;
import com.tailfriends.animal.dto.CharacterResponse;
import com.tailfriends.animal.service.CharacterService;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.species.domain.Shape;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CharacterController.class)
@WebMvcTestSupport
class CharacterControllerTest {

    private static final String BODY = """
            { "renderStyle": "ILLUSTRATION_2D", "shape": "gecko", "patternType": "spots",
              "colors": { "body": "#E0A33A", "pattern": "#3A2A1E", "belly": "#F2E6C8", "eye": "#2B2B2B" },
              "extra": { "headSize": 0.6, "tailWidth": 0.8, "patternDensity": 0.4 } }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterService characterService;

    @Test
    @WithMockUserId(7L)
    void 명세_예시대로_소문자_값을_받아_저장한다() throws Exception {
        given(characterService.save(eq(12L), any())).willReturn(new CharacterResponse(
                RenderStyle.ILLUSTRATION_2D, "gecko", "spots",
                new CharacterResponse.Colors("#E0A33A", "#3A2A1E", "#F2E6C8", "#2B2B2B"),
                new CharacterResponse.Extra(new BigDecimal("0.60"), new BigDecimal("0.80"), new BigDecimal("0.40"))));

        mockMvc.perform(put("/api/v1/animals/12/character").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shape").value("gecko"))
                .andExpect(jsonPath("$.data.colors.body").value("#E0A33A"))
                .andExpect(jsonPath("$.data.extra.headSize").value(0.6));
        verify(characterService).save(eq(12L), argThat(request ->
                request.shape() == Shape.GECKO && request.patternType() == PatternType.SPOTS));
    }

    @Test
    @WithMockUserId
    void 색이_RRGGBB가_아니면_400() throws Exception {
        mockMvc.perform(put("/api/v1/animals/12/character").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("#E0A33A", "orange")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId
    void extra가_0에서_1을_벗어나면_400() throws Exception {
        mockMvc.perform(put("/api/v1/animals/12/character").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"headSize\": 0.6", "\"headSize\": 1.2")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId
    void 무늬가_정해진_값이_아니면_400() throws Exception {
        mockMvc.perform(put("/api/v1/animals/12/character").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("spots", "zigzag")))
                .andExpect(status().isBadRequest());
    }
}
