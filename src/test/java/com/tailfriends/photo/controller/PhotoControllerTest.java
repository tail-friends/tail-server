package com.tailfriends.photo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.photo.domain.PhotoPurpose;
import com.tailfriends.photo.dto.PhotoUploadResponse;
import com.tailfriends.photo.service.PhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PhotoController.class)
@WebMvcTestSupport
class PhotoControllerTest {

    private static final MockMultipartFile FILE =
            new MockMultipartFile("file", "mango.jpg", "image/jpeg", new byte[] {1, 2, 3});

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoService photoService;

    @Test
    @WithMockUserId(1L)
    void 사진을_올리면_201과_썸네일_주소를_돌려준다() throws Exception {
        given(photoService.upload(eq(1L), eq(12L), eq(PhotoPurpose.CHARACTER), eq(true), any()))
                .willReturn(new PhotoUploadResponse(301L, "http://localhost:8080/files/photos/a_thumb.jpg", true));

        mockMvc.perform(multipart("/api/v1/photos").file(FILE)
                        .param("animalId", "12").param("purpose", "CHARACTER").param("keepOriginal", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.photoId").value(301))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("http://localhost:8080/files/photos/a_thumb.jpg"))
                .andExpect(jsonPath("$.data.originalKept").value(true));
    }

    @Test
    @WithMockUserId(1L)
    void keepOriginal을_안_보내면_보관하지_않는다() throws Exception {
        given(photoService.upload(eq(1L), eq(12L), eq(PhotoPurpose.ALBUM), eq(false), any()))
                .willReturn(new PhotoUploadResponse(302L, "http://localhost:8080/files/photos/b_thumb.jpg", false));

        mockMvc.perform(multipart("/api/v1/photos").file(FILE).param("animalId", "12").param("purpose", "ALBUM"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalKept").value(false));
    }

    @Test
    @WithMockUserId(1L)
    void 파일이_없으면_400() throws Exception {
        mockMvc.perform(multipart("/api/v1/photos").param("animalId", "12").param("purpose", "ALBUM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(1L)
    void 없는_purpose면_400() throws Exception {
        mockMvc.perform(multipart("/api/v1/photos").file(FILE).param("animalId", "12").param("purpose", "SELFIE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @WithMockUserId(1L)
    void 사진이_아니면_400_INVALID_FILE() throws Exception {
        given(photoService.upload(eq(1L), eq(12L), eq(PhotoPurpose.ALBUM), eq(false), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_FILE));

        mockMvc.perform(multipart("/api/v1/photos").file(FILE).param("animalId", "12").param("purpose", "ALBUM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(multipart("/api/v1/photos").file(FILE).param("animalId", "12").param("purpose", "ALBUM"))
                .andExpect(status().isUnauthorized());
    }
}
