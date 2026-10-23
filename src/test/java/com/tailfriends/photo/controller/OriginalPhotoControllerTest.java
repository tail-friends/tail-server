package com.tailfriends.photo.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.exception.BusinessException;
import com.tailfriends.global.exception.ErrorCode;
import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.photo.dto.OriginalDeleteResponse;
import com.tailfriends.photo.service.OriginalPhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OriginalPhotoController.class)
@WebMvcTestSupport
class OriginalPhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OriginalPhotoService originalPhotoService;

    @Test
    @WithMockUserId(1L)
    void 지운_원본_수를_돌려준다() throws Exception {
        given(originalPhotoService.deleteAll()).willReturn(new OriginalDeleteResponse(14));

        mockMvc.perform(delete("/api/v1/users/me/originals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(14));
    }

    @Test
    @WithMockUserId(1L)
    void 사용자가_없으면_401() throws Exception {
        given(originalPhotoService.deleteAll()).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(delete("/api/v1/users/me/originals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 토큰이_없으면_401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/originals"))
                .andExpect(status().isUnauthorized());
    }
}
