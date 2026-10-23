package com.tailfriends.user.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import com.tailfriends.global.support.WithMockUserId;
import com.tailfriends.user.dto.ConsentRequest;
import com.tailfriends.user.dto.ConsentResponse;
import com.tailfriends.user.service.ConsentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConsentController.class)
@WebMvcTestSupport
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentService consentService;

    @Test
    @WithMockUserId(1L)
    void 동의_상태를_명세_형식으로_돌려준다() throws Exception {
        given(consentService.getConsents()).willReturn(new ConsentResponse(true, true, false, true));

        mockMvc.perform(get("/api/v1/auth/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacy").value(true))
                .andExpect(jsonPath("$.data.photoAnalysis").value(true))
                .andExpect(jsonPath("$.data.keepOriginal").value(false))
                .andExpect(jsonPath("$.data.notification").value(true));
    }

    @Test
    @WithMockUserId(1L)
    void 동의를_저장한다() throws Exception {
        given(consentService.saveConsents(eq(new ConsentRequest(true, true, false, false))))
                .willReturn(new ConsentResponse(true, true, false, false));

        mockMvc.perform(put("/api/v1/auth/consents").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "privacy": true, "photoAnalysis": true, "keepOriginal": false, "notification": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.privacy").value(true))
                .andExpect(jsonPath("$.data.notification").value(false));
    }

    @Test
    @WithMockUserId(1L)
    void 항목이_빠지면_400() throws Exception {
        mockMvc.perform(put("/api/v1/auth/consents").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"privacy\": true, \"photoAnalysis\": true, \"keepOriginal\": false }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/consents"))
                .andExpect(status().isUnauthorized());
    }
}
