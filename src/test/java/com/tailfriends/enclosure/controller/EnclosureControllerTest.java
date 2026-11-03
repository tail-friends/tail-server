package com.tailfriends.enclosure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.dto.BackgroundResponse;
import com.tailfriends.enclosure.dto.DecorationListResponse;
import com.tailfriends.enclosure.dto.DecorationResponse;
import com.tailfriends.enclosure.dto.EnclosureListResponse;
import com.tailfriends.enclosure.dto.EnclosureResponse;
import com.tailfriends.enclosure.dto.EnclosureSummaryResponse;
import com.tailfriends.enclosure.service.EnclosureService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EnclosureController.class)
@WebMvcTestSupport
class EnclosureControllerTest {

    private static final BackgroundResponse DESERT = new BackgroundResponse(BackgroundType.BUILT_IN, "desert");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnclosureService enclosureService;

    @Test
    @WithMockUserId(7L)
    void 사육장_목록을_명세_형식으로_돌려준다() throws Exception {
        given(enclosureService.getEnclosures()).willReturn(new EnclosureListResponse(List.of(
                new EnclosureSummaryResponse(3L, DESERT, 2))));

        mockMvc.perform(get("/api/v1/enclosures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enclosures[0].id").value(3))
                .andExpect(jsonPath("$.data.enclosures[0].background.type").value("BUILT_IN"))
                .andExpect(jsonPath("$.data.enclosures[0].background.theme").value("desert"))
                .andExpect(jsonPath("$.data.enclosures[0].decorationCount").value(2));
    }

    @Test
    @WithMockUserId(7L)
    void 사육장_상세를_명세_형식으로_돌려주고_없으면_404() throws Exception {
        given(enclosureService.getEnclosure(3L)).willReturn(new EnclosureResponse(3L, DESERT,
                List.of(new DecorationResponse(21L, 0.3, 0.85, false)), List.of()));
        given(enclosureService.getEnclosure(99L)).willThrow(new BusinessException(ErrorCode.ENCLOSURE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/enclosures/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.background.theme").value("desert"))
                .andExpect(jsonPath("$.data.decorations[0].itemId").value(21))
                .andExpect(jsonPath("$.data.decorations[0].x").value(0.3))
                .andExpect(jsonPath("$.data.decorations[0].y").value(0.85))
                .andExpect(jsonPath("$.data.decorations[0].flip").value(false))
                .andExpect(jsonPath("$.data.animalIds").isEmpty());
        mockMvc.perform(get("/api/v1/enclosures/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENCLOSURE_NOT_FOUND"));
    }

    @Test
    @WithMockUserId(7L)
    void 배치를_저장하면_저장된_장식을_돌려준다() throws Exception {
        given(enclosureService.updateDecorations(eq(3L), any())).willReturn(new DecorationListResponse(List.of(
                new DecorationResponse(21L, 0.3, 0.85, true))));

        mockMvc.perform(put("/api/v1/enclosures/3/decorations").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decorations": [ { "itemId": 21, "x": 0.3, "y": 0.85, "flip": true } ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decorations[0].itemId").value(21))
                .andExpect(jsonPath("$.data.decorations[0].flip").value(true));
        verify(enclosureService).updateDecorations(eq(3L), argThat(request -> request.decorations().size() == 1));
    }

    @Test
    @WithMockUserId(7L)
    void 사지_않은_아이템이면_403_ITEM_NOT_OWNED() throws Exception {
        given(enclosureService.updateDecorations(eq(3L), any()))
                .willThrow(new BusinessException(ErrorCode.ITEM_NOT_OWNED));

        mockMvc.perform(put("/api/v1/enclosures/3/decorations").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"decorations\": [ { \"itemId\": 99, \"x\": 0.3, \"y\": 0.85 } ] }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ITEM_NOT_OWNED"));
    }

    @Test
    @WithMockUserId(7L)
    void 좌표가_범위를_벗어나거나_필수값이_없거나_20개를_넘으면_400() throws Exception {
        String one = "{ \"itemId\": 21, \"x\": 0.3, \"y\": 0.85 }";
        for (String body : List.of(
                "{ \"decorations\": [ { \"itemId\": 21, \"x\": 1.01, \"y\": 0.5 } ] }",
                "{ \"decorations\": [ { \"itemId\": 21, \"x\": 0.5, \"y\": -0.1 } ] }",
                "{ \"decorations\": [ { \"x\": 0.5, \"y\": 0.5 } ] }",
                "{ \"decorations\": [ { \"itemId\": 21, \"y\": 0.5 } ] }",
                "{ \"decorations\": [ null ] }",
                "{ }",
                "{ \"decorations\": [ %s ] }".formatted(String.join(", ", Collections.nCopies(21, one))))) {
            mockMvc.perform(put("/api/v1/enclosures/3/decorations").contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }
        verify(enclosureService, never()).updateDecorations(any(), any());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/enclosures"))
                .andExpect(status().isUnauthorized());
    }
}
