package com.tailfriends.enclosure.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.enclosure.domain.BackgroundType;
import com.tailfriends.enclosure.dto.BackgroundResponse;
import com.tailfriends.enclosure.dto.DecorationListResponse;
import com.tailfriends.enclosure.dto.DecorationResponse;
import com.tailfriends.enclosure.dto.EnclosureCreateResponse;
import com.tailfriends.enclosure.dto.EnclosureListResponse;
import com.tailfriends.enclosure.dto.EnclosureResponse;
import com.tailfriends.enclosure.dto.EnclosureSummaryResponse;
import com.tailfriends.enclosure.service.EnclosureService;
import com.tailfriends.enclosure.service.PhotoThemeService;
import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.photo.ai.EnclosureObjectType;
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

    private static final BackgroundResponse DESERT = new BackgroundResponse.BuiltIn(BackgroundType.BUILT_IN, "desert");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnclosureService enclosureService;

    @MockitoBean
    private PhotoThemeService photoThemeService;

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
    @WithMockUserId(7L)
    void 기본_테마_배경은_type과_theme만_사진_테마는_사진과_색을_준다() throws Exception {
        given(enclosureService.getEnclosures()).willReturn(new EnclosureListResponse(List.of(
                new EnclosureSummaryResponse(3L, DESERT, 0),
                new EnclosureSummaryResponse(9L,
                        new BackgroundResponse.Photo(BackgroundType.PHOTO, null, "#C9A26B", "#5B4A3A", null), 0))));

        mockMvc.perform(get("/api/v1/enclosures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enclosures[0].background.photoId").doesNotExist())
                .andExpect(jsonPath("$.data.enclosures[1].background.type").value("PHOTO"))
                .andExpect(jsonPath("$.data.enclosures[1].background.theme").doesNotExist())
                .andExpect(jsonPath("$.data.enclosures[1].background.photoId").value(nullValue()))
                .andExpect(jsonPath("$.data.enclosures[1].background.floorColor").value("#C9A26B"))
                .andExpect(jsonPath("$.data.enclosures[1].background.objects").doesNotExist());
    }

    @Test
    @WithMockUserId(7L)
    void 사진_테마_상세는_배경에_objects를_준다() throws Exception {
        given(enclosureService.getEnclosure(9L)).willReturn(new EnclosureResponse(9L,
                new BackgroundResponse.Photo(BackgroundType.PHOTO, 310L, "#C9A26B", "#5B4A3A",
                        List.of(new BackgroundResponse.ObjectResponse("ROCK", 0.7, 0.8))),
                List.of(), List.of()));

        mockMvc.perform(get("/api/v1/enclosures/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.background.photoId").value(310))
                .andExpect(jsonPath("$.data.background.wallColor").value("#5B4A3A"))
                .andExpect(jsonPath("$.data.background.objects[0].type").value("ROCK"))
                .andExpect(jsonPath("$.data.background.objects[0].x").value(0.7));
    }

    @Test
    @WithMockUserId(7L)
    void 사진_테마를_저장하면_201과_ID를_돌려준다() throws Exception {
        given(photoThemeService.create(any())).willReturn(new EnclosureCreateResponse(7L));

        mockMvc.perform(post("/api/v1/enclosures").contentType(MediaType.APPLICATION_JSON).content("""
                        { "photoId": 310, "floorColor": "#C9A26B", "wallColor": "#5B4A3A",
                          "objects": [ { "type": "ROCK", "x": 0.7, "y": 0.8 } ], "setAsHomeFor": 12 }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(7));

        verify(photoThemeService).create(argThat(request -> request.setAsHomeFor() == 12L
                && request.objects().getFirst().type() == EnclosureObjectType.ROCK));
    }

    @Test
    @WithMockUserId(7L)
    void 사진_테마의_색_형식이_틀리거나_장식이_10개를_넘거나_종류를_모르면_400() throws Exception {
        String object = "{ \"type\": \"ROCK\", \"x\": 0.5, \"y\": 0.5 }";
        for (String body : List.of(
                "{ \"photoId\": 310, \"floorColor\": \"sand\", \"wallColor\": \"#5B4A3A\" }",
                "{ \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }",
                "{ \"photoId\": 310, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\", \"objects\": [ %s ] }"
                        .formatted(String.join(", ", Collections.nCopies(11, object))),
                "{ \"photoId\": 310, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\", "
                        + "\"objects\": [ { \"type\": \"CAT\", \"x\": 0.5, \"y\": 0.5 } ] }")) {
            mockMvc.perform(post("/api/v1/enclosures").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }

        verify(photoThemeService, never()).create(any());
    }

    @Test
    @WithMockUserId(7L)
    void 사진_테마가_10개면_409() throws Exception {
        given(photoThemeService.create(any())).willThrow(new BusinessException(ErrorCode.ENCLOSURE_LIMIT_EXCEEDED));

        mockMvc.perform(post("/api/v1/enclosures").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"photoId\": 310, \"floorColor\": \"#C9A26B\", \"wallColor\": \"#5B4A3A\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ENCLOSURE_LIMIT_EXCEEDED"));
    }

    @Test
    @WithMockUserId(7L)
    void 사진_테마를_지우면_본문_없이_204_기본_테마면_400() throws Exception {
        willThrow(new BusinessException(ErrorCode.BUILT_IN_THEME)).given(photoThemeService).delete(3L);

        mockMvc.perform(delete("/api/v1/enclosures/9"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete("/api/v1/enclosures/3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUILT_IN_THEME"));

        verify(photoThemeService).delete(9L);
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/enclosures"))
                .andExpect(status().isUnauthorized());
    }
}
