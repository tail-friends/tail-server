package com.tailfriends.photo.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.AlbumPhotoResponse;
import com.tailfriends.photo.dto.AlbumResponse;
import com.tailfriends.photo.service.AlbumService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlbumController.class)
@WebMvcTestSupport
class AlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlbumService albumService;

    @Test
    @WithMockUserId(7L)
    void 성장_앨범을_명세_형식으로_돌려준다() throws Exception {
        given(albumService.getAlbum(12L, null, 20)).willReturn(new AlbumResponse(List.of(new AlbumPhotoResponse(
                301L, LocalDate.of(2026, 10, 18), "https://files/photos/301_thumb.jpg", PhotoPurpose.HEALTH,
                new BigDecimal("57.8"), null, true)), null));

        mockMvc.perform(get("/api/v1/photos").param("animalId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photos[0].photoId").value(301))
                .andExpect(jsonPath("$.data.photos[0].takenAt").value("2026-10-18"))
                .andExpect(jsonPath("$.data.photos[0].thumbnailUrl").value("https://files/photos/301_thumb.jpg"))
                .andExpect(jsonPath("$.data.photos[0].purpose").value("HEALTH"))
                .andExpect(jsonPath("$.data.photos[0].weightAtTime").value(57.8))
                .andExpect(jsonPath("$.data.photos[0].healthCheck").isEmpty())
                .andExpect(jsonPath("$.data.photos[0].originalKept").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());
    }

    @Test
    @WithMockUserId(7L)
    void animalId가_없거나_size가_범위_밖이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(get("/api/v1/photos").param("animalId", "12").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserId(7L)
    void 남의_개체면_404() throws Exception {
        given(albumService.getAlbum(99L, null, 20)).willThrow(new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));

        mockMvc.perform(get("/api/v1/photos").param("animalId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANIMAL_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/photos").param("animalId", "12"))
                .andExpect(status().isUnauthorized());
    }
}
