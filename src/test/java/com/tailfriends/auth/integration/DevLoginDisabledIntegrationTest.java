package com.tailfriends.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tailfriends.global.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class DevLoginDisabledIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 설정을_켜지_않으면_테스트_로그인은_없다() throws Exception {
        mockMvc.perform(post("/api/v1/dev/login").contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"x\" }"))
                .andExpect(status().isNotFound());
    }
}
