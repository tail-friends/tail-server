package com.tailfriends.global.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.WebMvcTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthCheckController.class)
@WebMvcTestSupport
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 인증_없이_헬스_체크를_호출하면_ok를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/health-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("ok"))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    void 인증이_필요한_경로를_토큰_없이_호출하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/animals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
