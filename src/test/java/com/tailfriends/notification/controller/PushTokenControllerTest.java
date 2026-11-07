package com.tailfriends.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.notification.domain.PushPlatform;
import com.tailfriends.notification.dto.PushTokenRequest;
import com.tailfriends.notification.service.PushTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PushTokenController.class)
@WebMvcTestSupport
class PushTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PushTokenService pushTokenService;

    @Test
    @WithMockUserId
    void 토큰을_등록하면_204() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/push-tokens").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"fcm-token\", \"platform\": \"IOS\" }"))
                .andExpect(status().isNoContent());

        verify(pushTokenService).register(new PushTokenRequest("fcm-token", PushPlatform.IOS));
    }

    @Test
    @WithMockUserId
    void 토큰이_비었거나_플랫폼이_목록에_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/push-tokens").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"\", \"platform\": \"IOS\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        mockMvc.perform(post("/api/v1/users/me/push-tokens").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"fcm-token\", \"platform\": \"WINDOWS\" }"))
                .andExpect(status().isBadRequest());
        verify(pushTokenService, never()).register(any());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/push-tokens").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"fcm-token\", \"platform\": \"IOS\" }"))
                .andExpect(status().isUnauthorized());
    }
}
